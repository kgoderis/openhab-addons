package org.openhab.io.homekit.bridge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.yaml.snakeyaml.Yaml;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.ItemRegistryChangeListener;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.MetadataRegistry;
import org.openhab.core.items.StateChangeListener;
import org.openhab.core.items.events.ItemEventFactory;
import org.openhab.core.items.events.ItemStateEvent;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.thing.UID;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristic;
import org.openhab.io.homekit.api.event.HomekitEvent;
import org.openhab.io.homekit.api.event.HomekitEventType;
import org.openhab.io.homekit.api.factory.HomekitAccessoryFactory;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.factory.HomekitServiceFactory;
import org.openhab.io.homekit.api.registry.HomekitAccessoryServerRegistry;
import org.openhab.io.homekit.api.server.HomekitAccessoryServer;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.accessory.HomekitAccessoryRegistryImpl;
import org.openhab.io.homekit.core.characteristic.AbstractHomekitCharacteristic;
import org.openhab.io.homekit.event.core.HomekitEventMetadata;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.event.model.characteristic.HomekitCharacteristicChangedEvent;
import org.openhab.io.homekit.event.model.characteristic.HomekitCharacteristicUpdateEvent;
import org.openhab.io.homekit.event.util.HomekitPeerGroupUID;
import org.openhab.io.homekit.util.HomekitUID;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openhab.io.homekit.bridge.HomekitItemConfigParser;
import org.openhab.io.homekit.bridge.HomekitTaggedItem;
import org.openhab.io.homekit.config.HomekitConfigurationManager;
import org.openhab.io.homekit.bridge.ConfigurationSource;

/**
 * The {@link HomekitItemBridge} manages the integration between openHAB items and Homekit accessories.
 * It handles the complete lifecycle of Homekit accessories, including creation, updates, and removal.
 * 
 * <p>
 * This class implements both {@link ItemRegistryChangeListener} and {@link StateChangeListener} to handle:
 * <ul>
 * <li>Item lifecycle events (addition, removal, updates)</li>
 * <li>State changes for Homekit-enabled items</li>
 * <li>HomekitCharacteristic value updates</li>
 * <li>HomekitAccessory registration and cleanup</li>
 * </ul>
 * </p>
 *
 * <p>
 * The bridge maintains thread-safe collections for:
 * <ul>
 * <li>Accessories ({@link #accessoryMap})</li>
 * <li>Characteristics ({@link #characteristicMap})</li>
 * <li>Factories ({@link #homekitFactories})</li>
 * </ul>
 * Synchronization is handled through dedicated locks for each collection.
 * </p>
 *
 * <p>
 * Key features:
 * <ul>
 * <li>Automatic accessory creation for tagged items</li>
 * <li>Support for group items and characteristic mapping</li>
 * <li>Thread-safe state management</li>
 * <li>Error handling and logging</li>
 * <li>Resource cleanup on deactivation</li>
 * </ul>
 * </p>
 *
 * @author Your Name - Initial contribution
 * @since 1.0.0
 */
@Component(service = HomekitItemBridge.class, immediate = true)
@NonNullByDefault
public class HomekitItemBridge implements ItemRegistryChangeListener, StateChangeListener {

    private static final Logger logger = LoggerFactory.getLogger(HomekitItemBridge.class);

    private static final boolean ENABLE_EXIT_EVENT_STATISTICS = true;
    private static final int MAX_STATISTICS_ENTRIES = 1000;
    private static final int STATISTICS_REPORT_INTERVAL_SECONDS = 60;

    private static final String LOG_PREFIX = "Homekit Bridge: ";
    private static final String LOG_STATE = LOG_PREFIX + "State - ";
    private static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    private static final String LOG_ACCESSORY = LOG_PREFIX + "HomekitAccessory - ";
    private static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    private static final String LOG_WARN = LOG_PREFIX + "Warning - ";
    private static final String LOG_DEBUG = LOG_PREFIX + "Debug - ";
    private static final String LOG_TRACE = LOG_PREFIX + "Trace - ";

    // Error messages
    private static final String ERROR_CREATING_ACCESSORY = LOG_ERROR
            + "Error creating Homekit accessory for item {}: {}";
    private static final String ERROR_UPDATING_CHARACTERISTIC = LOG_ERROR + "Error updating characteristic {}: {}";
    private static final String ERROR_REMOVING_ACCESSORY = LOG_ERROR + "Error removing accessory {}: {}";
    private static final String NO_AVAILABLE_SERVER = LOG_WARN
            + "No available bridge accessory server found for item {}";
    private static final String CHARACTERISTIC_CREATION_FAILED = LOG_ERROR
            + "Failed to create characteristic {} for item {}";
    private static final String SERVICE_CREATION_FAILED = LOG_ERROR + "Failed to create service {} for item {}";

    // Debug messages
    private static final String DEBUG_ACCESSORY_CREATED = LOG_ACCESSORY
            + "Successfully created Homekit accessory for item {}";
    private static final String DEBUG_UPDATING_CHARACTERISTIC = LOG_STATE
            + "Updating characteristic {} for item {} with value {}";
    private static final String DEBUG_REMOVING_ACCESSORY = LOG_ACCESSORY + "Removing Homekit accessory for item {}";
    private static final String DEBUG_ACCESSORY_REMOVED = LOG_ACCESSORY
            + "Successfully removed Homekit accessory for item {}";
    private static final String DEBUG_FOUND_COMPATIBLE_FACTORY = LOG_CONFIG + "Found compatible factory {} for item {}";
    private static final String DEBUG_FOUND_AVAILABLE_SERVER = LOG_CONFIG + "Found available server {} for item {}";

    // Thread safety
    private final Object accessoryLock = new Object();
    private final Object characteristicLock = new Object();

    private final ItemRegistry itemRegistry;
    private final EventPublisher eventPublisher;
    private final HomekitAccessoryRegistryImpl accessoryRegistry;
    private final MetadataRegistry metadataRegistry;
    private final HomekitAccessoryServerRegistry accessoryServerRegistry;
    private final Map<String, Collection<HomekitCharacteristic<?>>> characteristicMap = new ConcurrentHashMap<>();
    private final Map<String, HomekitAccessory> accessoryMap = new ConcurrentHashMap<>();
    private final HomekitEventManager eventManager;
    private final UID bridgeUID = new HomekitUID("bridge");
    private final Set<UID> peerGroup;
    private final Map<String, @Nullable ExitEvent> exitEvents;
    private final ExitEventStatisticsCollector statisticsCollector;
    private final HomekitServiceFactory serviceFactory;
    private final HomekitCharacteristicFactory characteristicFactory;
    private final HomekitAccessoryFactory accessoryFactory;
    private static final String YAML_FILE_NAME = "homekit-2.x-items.yaml";
    private final HomekitItemConfigParser configParser;
    private final HomekitConfigurationManager configManager;

    /**
     * Activates the bridge component and initializes necessary resources.
     * 
     * @param itemRegistry The item registry service
     * @param eventPublisher The event publisher service
     * @param accessoryRegistry The accessory registry service
     * @param accessoryServerRegistry The accessory server registry service
     */
    @Activate
    public HomekitItemBridge(@Reference ItemRegistry itemRegistry, @Reference EventPublisher eventPublisher,
            @Reference HomekitAccessoryRegistryImpl accessoryRegistry, @Reference MetadataRegistry metadataRegistry,
            @Reference HomekitAccessoryServerRegistry accessoryServerRegistry,
            @Reference HomekitEventManager eventManager, @Reference HomekitAccessoryFactory accessoryFactory,
            @Reference HomekitServiceFactory serviceFactory,
            @Reference HomekitCharacteristicFactory characteristicFactory,
            @Reference HomekitConfigurationManager configManager) {
        this.itemRegistry = itemRegistry;
        this.eventPublisher = eventPublisher;
        this.accessoryRegistry = accessoryRegistry;
        this.metadataRegistry = metadataRegistry;
        this.accessoryServerRegistry = accessoryServerRegistry;
        this.eventManager = eventManager;
        this.accessoryFactory = accessoryFactory;
        this.serviceFactory = serviceFactory;
        this.characteristicFactory = characteristicFactory;
        this.configParser = new HomekitItemConfigParser(metadataRegistry);
        this.configManager = configManager;

        initializeRegistryListener();

        // Initialize existing Homekit tagged items
        for (Item item : itemRegistry.getItems()) {
            HomekitTaggedItem taggedItem = new HomekitTaggedItem(item, itemRegistry, metadataRegistry, accessoryFactory,
                    serviceFactory, characteristicFactory);
            if (taggedItem.isTagged()) {
                createAccessoryForItem(taggedItem);
            }
        }

        this.peerGroup = Set.of(bridgeUID, new HomekitPeerGroupUID("openhab"), new HomekitPeerGroupUID("homekit"));
        this.exitEvents = new ConcurrentHashMap<>();
        this.statisticsCollector = new ExitEventStatisticsCollector();

        if (ENABLE_EXIT_EVENT_STATISTICS) {
            statisticsCollector.start();
        }
    }

    private void initializeRegistryListener() {
        itemRegistry.addRegistryChangeListener(this);
    }

    /**
     * Deactivates the bridge component and cleans up resources.
     */
    @Deactivate
    protected void deactivate() {
        try {
            if (ENABLE_EXIT_EVENT_STATISTICS) {
                statisticsCollector.stop();
            }
            itemRegistry.removeRegistryChangeListener(this);
            cleanup();
        } catch (Exception e) {
            logger.error("Error during deactivation: {}", e.getMessage(), e);
        } finally {
            // Ensure all resources are cleaned up
            synchronized (accessoryLock) {
                accessoryMap.clear();
            }
            synchronized (characteristicLock) {
                characteristicMap.clear();
            }
        }
    }

    /**
     * Cleans up all resources and removes all accessories.
     * This method is synchronized to ensure thread-safe cleanup.
     */
    private void cleanup() {
        synchronized (accessoryLock) {
            accessoryMap.values().forEach(accessory -> {
                try {
                    if (accessory != null) {
                        accessoryRegistry.remove(accessory.getUID());
                    }
                } catch (Exception e) {
                    if (accessory != null) {
                        logger.error(ERROR_REMOVING_ACCESSORY, accessory.getUID(), e.getMessage(), e);
                    }
                }
            });
            accessoryMap.clear();
        }

        synchronized (characteristicLock) {
            characteristicMap.clear();
        }
    }

    /**
     * Safely adds a characteristic to the map.
     * This method is synchronized to ensure thread-safe characteristic management.
     * 
     * @param itemName The name of the item
     * @param characteristic The characteristic to add
     */
    private void addCharacteristic(String itemName, HomekitCharacteristic<?> characteristic) {
        synchronized (characteristicLock) {
            Collection<HomekitCharacteristic<?>> characteristics = characteristicMap.computeIfAbsent(itemName,
                    k -> new ArrayList<>());
            characteristics.add(characteristic);
        }
    }

    /**
     * Safely removes a characteristic from the map.
     * This method is synchronized to ensure thread-safe characteristic management.
     * 
     * @param itemName The name of the item
     * @param characteristic The characteristic to remove
     */
    private void removeCharacteristic(String itemName, HomekitCharacteristic<?> characteristic) {
        synchronized (characteristicLock) {
            @Nullable
            Collection<HomekitCharacteristic<?>> characteristics = characteristicMap.get(itemName);
            if (characteristics != null) {
                characteristics.remove(characteristic);
                if (characteristics.isEmpty()) {
                    characteristicMap.remove(itemName);
                }
            }
        }
    }

    /**
     * Creates a new accessory for the given tagged item.
     * This method handles the complete accessory creation process including service and characteristic setup.
     * 
     * @param taggedItem The item to create an accessory for
     */
    private void createAccessoryForItem(HomekitTaggedItem taggedItem) {
        logger.trace("{}Entering createAccessoryForItem for item: {}", LOG_TRACE, taggedItem.getName());

        try {
            logger.debug("{}Starting factory compatibility check for item: {}", LOG_DEBUG, taggedItem.getName());
            logger.trace("{}Item details - Type: {}, Tags: {}", LOG_TRACE, taggedItem.getItem().getType(),
                    taggedItem.getHomekitTags());

            String serviceType = taggedItem.getServiceTag();
            if (serviceType == null) {
                logger.warn("{}No service type found for item: {} (type: {})", LOG_ERROR, taggedItem.getName(),
                        taggedItem.getItem().getType());
                logger.debug("{}Skipping item processing due to missing service type", LOG_DEBUG);
                logger.trace("{}Exiting createAccessoryForItem early due to missing service type", LOG_TRACE);
                return;
            }

            HomekitAccessoryServer server = accessoryServerRegistry.getAvailableBridgeAccessoryServer();
            if (server != null) {
                logger.debug(DEBUG_FOUND_AVAILABLE_SERVER, server.getUID(), taggedItem.getName());
                createAndRegisterAccessory(taggedItem, server);
            } else {
                logger.warn(NO_AVAILABLE_SERVER, taggedItem.getName());
            }

            logger.debug("{}Successfully processed item: {}", LOG_DEBUG, taggedItem.getName());
            logger.trace("{}Exiting createAccessoryForItem successfully", LOG_TRACE);

        } catch (Exception e) {
            logger.warn("{}Failed to process item {}: {}", LOG_ERROR, taggedItem.getName(), e.getMessage());
            Throwable cause = e.getCause();
            logger.debug("{}Error details - Type: {}, Cause: {}", LOG_DEBUG, e.getClass().getSimpleName(),
                    cause != null ? cause.getMessage() : "none");
            logger.trace("{}Stack trace for item processing failure:", LOG_TRACE, e);
            logger.trace("{}Exiting createAccessoryForItem with error", LOG_TRACE);
        }
    }

    /**
     * Creates and registers an accessory for the given tagged item.
     * This method is synchronized to ensure thread-safe accessory management.
     * 
     * @param taggedItem The item to create an accessory for
     * @param server The server to register the accessory with
     */
    private void createAndRegisterAccessory(HomekitTaggedItem taggedItem, HomekitAccessoryServer server) {
        logger.trace("{}Entering createAndRegisterAccessory", LOG_TRACE);

        try {
            logger.debug("{}Creating accessory for item: {}", LOG_DEBUG, taggedItem.getName());
            Optional<HomekitAccessory> accessory = createAccessory(taggedItem, server);
            if (accessory.isPresent()) {
                synchronized (accessoryLock) {
                    registerAccessory(taggedItem, accessory.get());
                }
                logger.debug(DEBUG_ACCESSORY_CREATED, taggedItem.getName());
            }
            logger.debug("{}Successfully processed item with factory", LOG_DEBUG);
            logger.trace("{}Exiting createAndRegisterAccessory successfully", LOG_TRACE);
        } catch (Exception e) {
            logger.error(ERROR_CREATING_ACCESSORY, taggedItem.getName(), e.getMessage(), e);
            logger.trace("{}Exiting createAndRegisterAccessory with error", LOG_TRACE);
        }
    }

    /**
     * Registers the created accessory with the registry.
     * This method is synchronized to ensure thread-safe registration.
     * 
     * @param taggedItem The item associated with the accessory
     * @param accessory The accessory to register
     */
    private void registerAccessory(HomekitTaggedItem taggedItem, HomekitAccessory accessory) {
        try {
            synchronized (accessoryLock) {
                accessoryRegistry.add(accessory);
                accessoryMap.put(taggedItem.getName(), accessory);
            }
            logger.debug("{}Registered Homekit accessory for item {} with UID {}", LOG_ACCESSORY, taggedItem.getName(),
                    accessory.getUID());
        } catch (Exception e) {
            logger.error(ERROR_CREATING_ACCESSORY, taggedItem.getName(), e.getMessage(), e);
        }
    }

    /**
     * Removes an accessory for the given item.
     * This method is synchronized to ensure thread-safe removal.
     * 
     * @param item The item whose accessory should be removed
     */
    private void removeAccessoryForItem(Item item) {
        logger.debug(DEBUG_REMOVING_ACCESSORY, item.getName());

        synchronized (accessoryLock) {
            @Nullable
            HomekitAccessory accessory = accessoryMap.remove(item.getName());
            if (accessory != null) {
                try {
                    accessoryRegistry.remove(accessory.getUID());
                    accessory.getServices().forEach(service -> {
                        service.getCharacteristics().forEach(characteristic -> {
                            synchronized (characteristicLock) {
                                removeCharacteristic(item.getName(), characteristic);
                            }
                        });
                    });
                    logger.debug(DEBUG_ACCESSORY_REMOVED, item.getName());
                } catch (Exception e) {
                    logger.error(ERROR_REMOVING_ACCESSORY, accessory.getUID(), e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Creates an accessory for the given tagged item.
     * 
     * @param taggedItem The item to create an accessory for
     * @param server The server to add the accessory to
     * @return The created accessory, or empty if creation failed
     */
    private Optional<HomekitAccessory> createAccessory(HomekitTaggedItem taggedItem, HomekitAccessoryServer server) {
        logger.trace("{}Entering createAccessory", LOG_TRACE);

        try {
            logger.debug("{}Creating accessory for item: {}", LOG_DEBUG, taggedItem.getName());
            Optional<HomekitTaggedItem> primaryAccessory = getPrimaryAccessory(taggedItem, taggedItem.getServiceTag(),
                    itemRegistry);
            Map<String, Item> characteristicItems = getCharacteristicTypeItemMap(taggedItem);

            if (primaryAccessory.isPresent()) {
                HomekitAccessory accessory = accessoryFactory.createAccessoryFromTag("generic");
                accessory.assignToServer(server);
                Optional<HomekitService> primaryService = createPrimaryService(primaryAccessory.get(), accessory,
                        taggedItem);
                if (primaryService.isPresent()) {
                    accessory.addService(primaryService.get());
                    addCharacteristics(primaryService.get(), characteristicItems, accessory);
                    return Optional.of(accessory);
                }
            }
            logger.debug("{}Successfully processed item with factory", LOG_DEBUG);
            logger.trace("{}Exiting createAccessory successfully", LOG_TRACE);
            return Optional.empty();
        } catch (Exception e) {
            logger.warn(ERROR_CREATING_ACCESSORY, taggedItem.getName(), e.getMessage());
            logger.trace("{}Exiting createAccessory with error", LOG_TRACE);
            return Optional.empty();
        }
    }

    /**
     * Creates the primary service for an accessory.
     * 
     * @param primaryAccessoryItem The primary accessory item
     * @param accessory The accessory to add the service to
     * @param taggedItem The tagged item
     * @return The created service, or empty if creation failed
     */
    private Optional<HomekitService> createPrimaryService(HomekitTaggedItem primaryAccessoryItem,
            HomekitAccessory accessory, HomekitTaggedItem taggedItem) {
        logger.trace("{}Entering createPrimaryService", LOG_TRACE);

        try {
            logger.debug("{}Creating primary service for item: {}", LOG_DEBUG, taggedItem.getName());
            HomekitService primaryService = serviceFactory.createServiceFromTag(primaryAccessoryItem.getServiceTag(),
                    accessory);
            primaryService.withInstanceId(accessory.getNextAvailableInstanceId())
                    .withName("Primary HomekitService for " + taggedItem.getItem().getName()).withExtensible(true);

            logger.debug("{}Successfully created primary service for item: {}", LOG_DEBUG, taggedItem.getName());
            logger.trace("{}Exiting createPrimaryService successfully", LOG_TRACE);
            return Optional.ofNullable(primaryService);
        } catch (Exception e) {
            logger.warn(SERVICE_CREATION_FAILED, primaryAccessoryItem.getServiceTag(), taggedItem.getName());
            logger.trace("{}Exiting createPrimaryService with error", LOG_TRACE);
            return Optional.empty();
        }
    }

    /**
     * Adds characteristics to the primary service.
     * 
     * @param service The service to add characteristics to
     * @param characteristicItems Map of characteristic types to items
     * @param accessory The accessory containing the service
     */
    private void addCharacteristics(HomekitService service, Map<String, Item> characteristicItems,
            HomekitAccessory accessory) {
        for (Map.Entry<String, Item> entry : characteristicItems.entrySet()) {
            @Nullable
            String characteristicTag = entry.getKey();
            @Nullable
            Item item = entry.getValue();

            if (shouldAddCharacteristic(service, characteristicTag)) {
                addCharacteristicToService(service, characteristicTag, item, accessory);
            }
        }
    }

    private boolean shouldAddCharacteristic(HomekitService primaryService, String characteristicTag) {
        return primaryService.isExtensible()
                && !primaryService.getCharacteristics().stream().anyMatch(c -> c.getTag().equals(characteristicTag));
    }

    private void addCharacteristicToService(HomekitService service, String characteristicTag, Item item,
            HomekitAccessory accessory) {
        try {
            logger.debug("{}Adding characteristic {} to service", LOG_DEBUG, characteristicTag);
            HomekitCharacteristic<?> characteristic = characteristicFactory
                    .createCharacteristicFromTag(characteristicTag, service);
            if (characteristic != null) {
                service.addCharacteristic(characteristic);
                addCharacteristic(item.getName(), characteristic);
                subscribeToEvents(characteristic, item);
            }
            logger.debug("{}Successfully added characteristic {} to service", LOG_DEBUG, characteristicTag);
        } catch (Exception e) {
            logger.warn(CHARACTERISTIC_CREATION_FAILED, characteristicTag, item.getName());
        }
    }

    /**
     * Sets up the listener for a characteristic.
     * 
     * @param characteristic The characteristic to set up the listener for
     * @param item The item associated with the characteristic
     */
    private void subscribeToEvents(HomekitCharacteristic<?> characteristic, Item item) {
        if (!(characteristic instanceof AbstractHomekitCharacteristic<?> genericCharacteristic)) {
            return;
        }
        eventManager.subscribe(HomekitEventType.CHARACTERISTIC_VALUE_CHANGED, genericCharacteristic.getUID(), bridgeUID,
                event -> handleCharacteristicValueChangedEvent(event, item));
    }

    private void handleCharacteristicValueChangedEvent(HomekitEvent event, Item item) {
        if (event.getType() != HomekitEventType.CHARACTERISTIC_STATE_CHANGED) {
            return;
        }
        if (!(event instanceof HomekitCharacteristicChangedEvent)) {
            return;
        }

        // Check if event is from peer group
        if (event.getMetadata().isFromPeerGroup(peerGroup)) {
            logger.debug("Ignoring Homekit event from peer group: {}", event.getMetadata().getImmediateOrigin());
            return;
        }

        HomekitCharacteristic<?> eventCharacteristic = ((HomekitCharacteristicChangedEvent) event).getCharacteristic()
                .get();
        State newState = eventCharacteristic.toState(((HomekitCharacteristicChangedEvent) event).getNewValue().get());
        if (newState != null) {
            // Store the exit event
            exitEvents.put(item.getName(), new ExitEvent(newState, event.getMetadata()));

            // Post the state change to OpenHAB
            eventPublisher.post(ItemEventFactory.createStateEvent(item.getName(), newState));
        }
    }

    /**
     * Given an accessory group, return the item in the group tagged as an accessory.
     *
     * @param taggedItem The group item containing our item, or, the accessory item
     * @param serviceType The accessory type for which we're looking
     * @param itemRegistry The item registry to use
     * @return Optional containing the primary accessory if found
     */
    private Optional<HomekitTaggedItem> getPrimaryAccessory(HomekitTaggedItem taggedItem, String serviceType,
            ItemRegistry itemRegistry) {
        if (taggedItem == null || serviceType == null || itemRegistry == null) {
            return Optional.empty();
        }

        logger.debug("{}: isGroup? {}, isMember? {}", taggedItem.getName(), taggedItem.isGroup(),
                taggedItem.isMemberOfAccessoryGroup());

        if (taggedItem.isGroup()) {
            GroupItem groupItem = (GroupItem) taggedItem.getItem();
            return groupItem.getMembers().stream().filter(item -> item.hasTag(serviceType))
                    .map(item -> new HomekitTaggedItem(item, itemRegistry, metadataRegistry, accessoryFactory,
                            serviceFactory, characteristicFactory))
                    .findFirst();
        } else if (serviceType.equals(taggedItem.getServiceTag())) {
            return Optional.of(taggedItem);
        }
        return Optional.empty();
    }

    /**
     * Gets the map of characteristic types to items for a tagged item.
     *
     * @param taggedItem The tagged item to get characteristics for
     * @return Map of characteristic types to items
     */
    private Map<String, Item> getCharacteristicTypeItemMap(HomekitTaggedItem taggedItem) {
        if (taggedItem.isGroup()) {
            GroupItem groupItem = (GroupItem) taggedItem.getItem();
            Map<String, Item> characteristicItems = new HashMap<>();
            groupItem.getMembers().forEach(item -> {
                String tag = getHomekitTags(item).stream().filter(aTag -> characteristicFactory.supportsTag(aTag))
                        .findFirst().orElse(null);
                if (tag != null) {
                    if (characteristicItems.containsKey(tag)) {
                        logger.warn("incorrect configuration for {} detected: {} and {} are tagged as {}, skipping {}",
                                taggedItem.getItem().getUID(),
                                java.util.Objects.requireNonNull(characteristicItems.get(tag)).getUID(), item.getUID(),
                                tag, item.getUID());
                    } else {
                        characteristicItems.put(tag, item);
                    }
                }
            });
            return Collections.unmodifiableMap(characteristicItems);
        } else {
            return Collections.emptyMap();
        }
    }

    // ========== Item Registry Change Listener Methods ==========
    /**
     * Handles the addition of a new item to the registry.
     * If the item is tagged for Homekit integration, creates a new accessory for it.
     * 
     * @param item The item that was added to the registry
     */
    @Override
    public void added(Item item) {
        HomekitTaggedItem taggedItem = new HomekitTaggedItem(item, itemRegistry, metadataRegistry, accessoryFactory,
                serviceFactory, characteristicFactory);
        if (taggedItem.isTagged()) {
            Map<String, Object> config = configParser.getFilteredConfig(item);
            configManager.updateConfiguration(item.getName(), config, HomekitConfigurationManager.ConfigurationType.ITEM, YAML_FILE_NAME);
            createAccessoryForItem(taggedItem);
        }
    }

     //TODO  :  add logic to prioritse configuration coming from the config yaml over the metadata

    /**
     * Handles the removal of an item from the registry.
     * Removes the associated Homekit accessory if it exists.
     * 
     * @param item The item that was removed from the registry
     */
    @Override
    public void removed(Item item) {
        configManager.removeConfiguration(item.getName(), HomekitConfigurationManager.ConfigurationType.ITEM);
        removeAccessoryForItem(item);
    }

    /**
     * Handles the update of an existing item in the registry.
     * Removes the old accessory and creates a new one if the updated item is tagged for Homekit integration.
     * 
     * @param oldItem The previous version of the item
     * @param item The updated version of the item
     */
    @Override
    public void updated(Item oldItem, Item item) {
        configManager.removeConfiguration(oldItem.getName(), HomekitConfigurationManager.ConfigurationType.ITEM);
        removeAccessoryForItem(oldItem);
        HomekitTaggedItem taggedItem = new HomekitTaggedItem(item, itemRegistry, metadataRegistry, accessoryFactory,
                serviceFactory, characteristicFactory);
        if (taggedItem.isTagged()) {
            Map<String, Object> config = configParser.getFilteredConfig(item);
            configManager.updateConfiguration(item.getName(), config, HomekitConfigurationManager.ConfigurationType.ITEM, YAML_FILE_NAME);
            createAccessoryForItem(taggedItem);
        }
    }

    /**
     * Called when all items in the registry have changed.
     * This method handles removing all old accessories and creating new ones for tagged items.
     * 
     * @param oldItemNames Collection of names of items that were previously in the registry
     */
    @Override
    public void allItemsChanged(Collection<String> oldItemNames) {
        // Potential concurrent modification if items are added/removed during iteration
        oldItemNames.forEach(itemName -> {
            Item item = itemRegistry.get(itemName);
            if (item != null) {
                removeAccessoryForItem(item);
            }
        });

        // Create new accessories for all tagged items
        itemRegistry.getItems().stream()
                .map(item -> new HomekitTaggedItem(item, itemRegistry, metadataRegistry, accessoryFactory,
                        serviceFactory, characteristicFactory))
                .filter(HomekitTaggedItem::isTagged).forEach(this::createAccessoryForItem);
    }

    // ========== State Change Listener Methods ==========
    /**
     * Handles state changes for items.
     * Updates the corresponding Homekit characteristic values when an item's state changes.
     * 
     * @param item The item whose state changed
     * @param oldState The previous state of the item
     * @param newState The new state of the item
     */
    @Override
    public void stateChanged(Item item, State oldState, State newState) {
        if (item == null || newState == null) {
            return;
        }

                            // get the homekit namespace metadata for the item
                            MetadataKey key = new MetadataKey("homekit", item.getName());
                            Metadata metadata = metadataRegistry.get(key);
                            Map<String, Object> itemConfiguration = metadata != null ? metadata.getConfiguration() : Collections.emptyMap();

        // Clean up expired exit events
        exitEvents.entrySet().removeIf(entry -> entry.getValue().isExpired());

        @Nullable
        Collection<HomekitCharacteristic<?>> characteristics = characteristicMap.get(item.getName());
        if (characteristics != null) {
            characteristics.forEach(c -> {
                try {
                    ExitEvent exitEvent = exitEvents.get(item.getName());

                    Optional.ofNullable(exitEvent).filter(e -> !e.isExpired()).ifPresent(e -> {
                        if (statesEqual(e.getState(), newState)) {
                            logger.debug("Processing correlated state change for item: {}", item.getName());

                            if (ENABLE_EXIT_EVENT_STATISTICS) {
                                statisticsCollector.recordEvent(System.currentTimeMillis() - e.getTimestamp());
                            }

    
                            HomekitEvent newEvent = new HomekitCharacteristicUpdateEvent(bridgeUID, c.getUID(), c,
                                    c.toValueJson(e.getState()), c.toValueJson(newState), itemConfiguration, e.getMetadata());

                            eventManager.publishEvent(newEvent);
                            exitEvents.remove(item.getName());
                        }
                    });

                    // Handle uncorrelated state change
                    if (exitEvent == null) {
                        logger.debug("Processing new state change for item: {}", item.getName());
                        HomekitEvent newEvent = new HomekitCharacteristicUpdateEvent(bridgeUID, c.getUID(), c,
                                c.toValueJson(oldState), c.toValueJson(newState), itemConfiguration,
                                new HomekitEventMetadata(bridgeUID, null, bridgeUID, peerGroup));
                        eventManager.publishEvent(newEvent);
                    }
                } catch (Exception e) {
                    logger.error(ERROR_UPDATING_CHARACTERISTIC, c.getType(), e.getMessage(), e);
                }
            });
        }
    }

    /**
     * Handles state updates for items.
     * Updates the corresponding Homekit characteristic values when an item's state is updated.
     * 
     * @param item The item whose state was updated
     * @param state The new state of the item
     */
    @Override
    public void stateUpdated(Item item, State state) {
        if (item == null || state == null) {
            return;
        }
        // get the homekit namespace metadata for the item
        MetadataKey key = new MetadataKey("homekit", item.getName());
        Metadata metadata = metadataRegistry.get(key);
        Map<String, Object> itemConfiguration = metadata != null ? metadata.getConfiguration() : Collections.emptyMap();

        Optional.ofNullable(characteristicMap.get(item.getName()))
                .ifPresent(characteristics -> characteristics.forEach(c -> {
                    try {
                        Optional.ofNullable(exitEvents.get(item.getName())).ifPresent(exitEvent -> {
                            HomekitEvent newEvent = new HomekitCharacteristicUpdateEvent(c,
                                    c.toValueJson(exitEvent.getState()), c.toValueJson(state), itemConfiguration, exitEvent.getMetadata());
                            eventManager.publishEvent(newEvent);
                        });
                    } catch (Exception e) {
                        logger.error(ERROR_UPDATING_CHARACTERISTIC, c.getType(), e.getMessage(), e);
                    }
                }));
    }

    private Collection<String> getHomekitTags(Item item) {
        MetadataKey key = new MetadataKey("homekit", item.getName());
        Metadata metadata = metadataRegistry.get(key);
        return metadata != null ? Arrays.asList(metadata.getValue().split(",")) : Collections.emptyList();
    }

    private boolean statesEqual(State state1, State state2) {
        if (state1 == state2)
            return true;
        if (state1 == null || state2 == null)
            return false;

        // Handle different state types appropriately
        if (state1 instanceof DecimalType && state2 instanceof DecimalType) {
            return ((DecimalType) state1).doubleValue() == ((DecimalType) state2).doubleValue();
        }
        if (state1 instanceof OnOffType && state2 instanceof OnOffType) {
            return state1 == state2;
        }
        // Add other state type comparisons as needed

        return state1.equals(state2);
    }

    /**
     * Internal class for tracking state changes and their metadata.
     */
    private static class ExitEvent {
        private final State state;
        private final HomekitEventMetadata metadata;
        private final long timestamp;
        private final long correlationWindowMs = 1000; // 1 second window

        public ExitEvent(State state, HomekitEventMetadata metadata) {
            this.state = state;
            this.metadata = metadata;
            this.timestamp = System.currentTimeMillis();
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > correlationWindowMs;
        }

        public State getState() {
            return state;
        }

        public HomekitEventMetadata getMetadata() {
            return metadata;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }

    /**
     * Internal class for collecting and analyzing statistics about exit events.
     */
    private class ExitEventStatisticsCollector {
        private final List<Long> eventTimes = new ArrayList<>();
        private final Object lock = new Object();
        private @Nullable ScheduledFuture<?> scheduledTask;
        private @Nullable ScheduledExecutorService executor;

        public void start() {
            executor = ThreadPoolManager.getScheduledPool("homekit");
            if (executor != null) {
                scheduledTask = executor.scheduleAtFixedRate(this::printStatistics, STATISTICS_REPORT_INTERVAL_SECONDS,
                        STATISTICS_REPORT_INTERVAL_SECONDS, TimeUnit.SECONDS);
            }
        }

        public void stop() {
            if (scheduledTask != null) {
                scheduledTask.cancel(false);
                scheduledTask = null;
            }
            executor = null;
        }

        public void recordEvent(long timeMs) {
            synchronized (lock) {
                if (eventTimes.size() >= MAX_STATISTICS_ENTRIES) {
                    eventTimes.remove(0);
                }
                eventTimes.add(timeMs);
            }
        }

        private void printStatistics() {
            synchronized (lock) {
                if (eventTimes.isEmpty()) {
                    logger.info("No exit event statistics available yet");
                    return;
                }

                // Calculate basic statistics
                double sum = 0;
                double sumSquared = 0;
                for (long time : eventTimes) {
                    sum += time;
                    sumSquared += time * time;
                }
                double mean = sum / eventTimes.size();
                double variance = (sumSquared / eventTimes.size()) - (mean * mean);
                double stdDev = Math.sqrt(variance);

                // Calculate deciles
                List<Long> sortedTimes = new ArrayList<>(eventTimes);
                Collections.sort(sortedTimes);
                int[] deciles = new int[11];
                for (int i = 0; i <= 10; i++) {
                    int index = (int) Math.round(i * (sortedTimes.size() - 1) / 10.0);
                    deciles[i] = sortedTimes.get(index).intValue();
                }

                // Build histogram
                StringBuilder histogram = new StringBuilder("\nExit Event Time Distribution (ms):\n");
                for (int i = 0; i < 10; i++) {
                    int count = 0;
                    for (long time : sortedTimes) {
                        if (time >= deciles[i] && time < deciles[i + 1]) {
                            count++;
                        }
                    }
                    double percentage = (count * 100.0) / sortedTimes.size();
                    histogram.append(String.format("%4d-%-4d ms: %3d%% (%d events)\n", deciles[i], deciles[i + 1],
                            (int) percentage, count));
                }

                logger.info(
                        "Exit Event Statistics (based on {} events):\n" + "Mean: {:.2f} ms\n" + "Std Dev: {:.2f} ms\n"
                                + "Min: {} ms\n" + "Max: {} ms\n" + "{}",
                        eventTimes.size(), mean, stdDev, sortedTimes.get(0), sortedTimes.get(sortedTimes.size() - 1),
                        histogram);
            }
        }
    }
}
