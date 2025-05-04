package org.openhab.io.homekit.internal.bridge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
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
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.factory.HomekitFactory;
import org.openhab.io.homekit.api.hap.Accessory;
import org.openhab.io.homekit.api.hap.AccessoryServer;
import org.openhab.io.homekit.api.hap.Characteristic;
import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.api.registry.AccessoryServerRegistry;
import org.openhab.io.homekit.internal.accessory.AccessoryRegistryImpl;
import org.openhab.io.homekit.internal.accessory.GenericAccessory;
import org.openhab.io.homekit.internal.characteristic.GenericCharacteristic;
import org.openhab.io.homekit.internal.events.CharacteristicEvent;
import org.openhab.io.homekit.internal.events.HomekitEvent;
import org.openhab.io.homekit.internal.events.HomekitEventManager;
import org.openhab.io.homekit.internal.events.HomekitEventType;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link HomekitItemBridge} manages the integration between openHAB items and HomeKit accessories.
 * It handles the complete lifecycle of HomeKit accessories, including creation, updates, and removal.
 * 
 * <p>
 * This class implements both {@link ItemRegistryChangeListener} and {@link StateChangeListener} to handle:
 * <ul>
 * <li>Item lifecycle events (addition, removal, updates)</li>
 * <li>State changes for HomeKit-enabled items</li>
 * <li>Characteristic value updates</li>
 * <li>Accessory registration and cleanup</li>
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

    private static final String LOG_PREFIX = "HomeKit Bridge: ";
    private static final String LOG_STATE = LOG_PREFIX + "State - ";
    private static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    private static final String LOG_ACCESSORY = LOG_PREFIX + "Accessory - ";
    private static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    private static final String LOG_WARN = LOG_PREFIX + "Warning - ";
    private static final String LOG_DEBUG = LOG_PREFIX + "Debug - ";
    private static final String LOG_TRACE = LOG_PREFIX + "Trace - ";

    // Error messages
    private static final String ERROR_CREATING_ACCESSORY = LOG_ERROR
            + "Error creating HomeKit accessory for item {}: {}";
    private static final String ERROR_UPDATING_CHARACTERISTIC = LOG_ERROR + "Error updating characteristic {}: {}";
    private static final String ERROR_REMOVING_ACCESSORY = LOG_ERROR + "Error removing accessory {}: {}";
    private static final String NO_AVAILABLE_SERVER = LOG_WARN
            + "No available bridge accessory server found for item {}";
    private static final String CHARACTERISTIC_CREATION_FAILED = LOG_ERROR
            + "Failed to create characteristic {} for item {}";
    private static final String SERVICE_CREATION_FAILED = LOG_ERROR + "Failed to create service {} for item {}";

    // Debug messages
    private static final String DEBUG_ACCESSORY_CREATED = LOG_ACCESSORY
            + "Successfully created HomeKit accessory for item {}";
    private static final String DEBUG_UPDATING_CHARACTERISTIC = LOG_STATE
            + "Updating characteristic {} for item {} with value {}";
    private static final String DEBUG_REMOVING_ACCESSORY = LOG_ACCESSORY + "Removing HomeKit accessory for item {}";
    private static final String DEBUG_ACCESSORY_REMOVED = LOG_ACCESSORY
            + "Successfully removed HomeKit accessory for item {}";
    private static final String DEBUG_FOUND_COMPATIBLE_FACTORY = LOG_CONFIG + "Found compatible factory {} for item {}";
    private static final String DEBUG_FOUND_AVAILABLE_SERVER = LOG_CONFIG + "Found available server {} for item {}";

    // Thread safety
    private final Object accessoryLock = new Object();
    private final Object characteristicLock = new Object();
    private final Object factoryLock = new Object();

    private final ItemRegistry itemRegistry;
    private final EventPublisher eventPublisher;
    private final AccessoryRegistryImpl accessoryRegistry;
    private final MetadataRegistry metadataRegistry;
    private final AccessoryServerRegistry accessoryServerRegistry;
    private final Set<HomekitFactory> homekitFactories = Collections.synchronizedSet(new HashSet<>());
    private final Map<String, Collection<Characteristic<?>>> characteristicMap = new ConcurrentHashMap<>();
    private final Map<String, Accessory> accessoryMap = new ConcurrentHashMap<>();
    private final HomekitEventManager eventManager;
    private final String subscriberUID = "bridge:" + UUID.randomUUID().toString();

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
            @Reference AccessoryRegistryImpl accessoryRegistry, @Reference MetadataRegistry metadataRegistry,
            @Reference AccessoryServerRegistry accessoryServerRegistry, @Reference HomekitEventManager eventManager) {
        this.itemRegistry = itemRegistry;
        this.eventPublisher = eventPublisher;
        this.accessoryRegistry = accessoryRegistry;
        this.metadataRegistry = metadataRegistry;
        this.accessoryServerRegistry = accessoryServerRegistry;
        this.eventManager = eventManager;

        initializeRegistryListener();

        // Initialize existing HomeKit tagged items
        for (Item item : itemRegistry.getItems()) {
            HomekitTaggedItem taggedItem = new HomekitTaggedItem(item, itemRegistry, metadataRegistry,
                    homekitFactories);
            if (taggedItem.isTagged()) {
                createAccessoryForItem(taggedItem);
            }
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
            synchronized (factoryLock) {
                homekitFactories.clear();
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

        synchronized (factoryLock) {
            homekitFactories.clear();
        }
    }

    /**
     * Safely adds a characteristic to the map.
     * This method is synchronized to ensure thread-safe characteristic management.
     * 
     * @param itemName The name of the item
     * @param characteristic The characteristic to add
     */
    private void addCharacteristic(String itemName, Characteristic<?> characteristic) {
        synchronized (characteristicLock) {
            @SuppressWarnings("null")
            Collection<Characteristic<?>> characteristics = characteristicMap.computeIfAbsent(itemName,
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
    private void removeCharacteristic(String itemName, Characteristic<?> characteristic) {
        synchronized (characteristicLock) {
            @Nullable
            Collection<Characteristic<?>> characteristics = characteristicMap.get(itemName);
            if (characteristics != null) {
                characteristics.remove(characteristic);
                if (characteristics.isEmpty()) {
                    characteristicMap.remove(itemName);
                }
            }
        }
    }

    /**
     * Adds a new HomeKit factory to the bridge.
     * This method is synchronized to ensure thread-safe factory management.
     * 
     * @param homekitFactory The factory to add
     */
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addHomekitFactory(HomekitFactory homekitFactory) {
        synchronized (factoryLock) {
            homekitFactories.add(homekitFactory);
        }

        // Check existing items for compatibility with new factory
        itemRegistry.getItems().stream()
                .map(item -> new HomekitTaggedItem(item, itemRegistry, metadataRegistry, homekitFactories))
                .filter(HomekitTaggedItem::isTagged).filter(taggedItem -> {
                    synchronized (accessoryLock) {
                        return !accessoryMap.containsKey(taggedItem.getName());
                    }
                }).forEach(this::createAccessoryForItem);
    }

    /**
     * Removes a HomeKit factory from the bridge.
     * This method is synchronized to ensure thread-safe factory management.
     * 
     * @param homekitFactory The factory to remove
     */
    protected void removeHomekitFactory(HomekitFactory homekitFactory) {
        synchronized (factoryLock) {
            homekitFactories.remove(homekitFactory);
        }
    }

    /**
     * Finds a compatible factory for the given tagged item.
     * 
     * @param taggedItem The item to find a compatible factory for
     * @return Optional containing the compatible factory if found
     */
    private Optional<HomekitFactory> findCompatibleFactory(HomekitTaggedItem taggedItem) {
        String serviceType = taggedItem.getServiceType();
        if (serviceType == null) {
            return Optional.empty();
        }

        synchronized (factoryLock) {
            return homekitFactories.stream().filter(factory -> factory.supportsServiceType(serviceType)).findFirst();
        }
    }

    /**
     * Finds a compatible factory for a characteristic type.
     * 
     * @param characteristicType The type of characteristic to find a factory for
     * @return Optional containing the compatible factory if found
     */
    private Optional<HomekitFactory> findCompatibleCharacteristicFactory(String characteristicType) {
        synchronized (factoryLock) {
            return homekitFactories.stream().filter(factory -> factory.supportsCharacteristicsType(characteristicType))
                    .findFirst();
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

            Optional<HomekitFactory> compatibleFactory = findCompatibleFactory(taggedItem);
            if (!compatibleFactory.isPresent()) {
                logger.warn("{}No compatible factory found for item: {} (type: {})", LOG_ERROR, taggedItem.getName(),
                        taggedItem.getItem().getType());
                logger.debug("{}Skipping item processing due to missing factory", LOG_DEBUG);
                logger.trace("{}Exiting createAccessoryForItem early due to missing factory", LOG_TRACE);
                return;
            }

            if (compatibleFactory.isPresent()) {
                @SuppressWarnings("null")
                HomekitFactory factory = compatibleFactory.get();
                logger.debug("{}Found compatible factory: {}", LOG_DEBUG, factory.getClass().getSimpleName());
                logger.trace("{}Factory details - Class: {}, Supported types: {}", LOG_TRACE,
                        factory.getClass().getName(), factory.getSupportedThingTypes());
            }

            compatibleFactory.ifPresent(serviceFactory -> {
                logger.debug(DEBUG_FOUND_COMPATIBLE_FACTORY, serviceFactory.getClass().getSimpleName(),
                        taggedItem.getName());

                AccessoryServer server = accessoryServerRegistry.getAvailableBridgeAccessoryServer();
                if (server != null) {
                    logger.debug(DEBUG_FOUND_AVAILABLE_SERVER, server.getUID(), taggedItem.getName());
                    createAndRegisterAccessory(taggedItem, serviceFactory, server);
                } else {
                    logger.warn(NO_AVAILABLE_SERVER, taggedItem.getName());
                }
            });

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
     * @param serviceFactory The factory to create the accessory
     * @param server The server to register the accessory with
     */
    private void createAndRegisterAccessory(HomekitTaggedItem taggedItem, HomekitFactory serviceFactory,
            AccessoryServer server) {
        logger.trace("{}Entering createAndRegisterAccessory", LOG_TRACE);

        try {
            logger.debug("{}Creating accessory for item: {}", LOG_DEBUG, taggedItem.getName());
            Optional<Accessory> accessory = createAccessory(taggedItem, serviceFactory, server);
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
    private void registerAccessory(HomekitTaggedItem taggedItem, Accessory accessory) {
        try {
            synchronized (accessoryLock) {
                accessoryRegistry.add(accessory);
                accessoryMap.put(taggedItem.getName(), accessory);
            }
            logger.debug("{}Registered HomeKit accessory for item {} with UID {}", LOG_ACCESSORY, taggedItem.getName(),
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
            Accessory accessory = accessoryMap.remove(item.getName());
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
     * @param serviceFactory The factory to create the accessory
     * @param server The server to add the accessory to
     * @return The created accessory, or empty if creation failed
     */
    @SuppressWarnings("null")
    private Optional<Accessory> createAccessory(HomekitTaggedItem taggedItem, HomekitFactory serviceFactory,
            AccessoryServer server) {
        logger.trace("{}Entering createAccessory", LOG_TRACE);

        try {
            logger.debug("{}Creating accessory for item: {}", LOG_DEBUG, taggedItem.getName());
            @Nullable
            HomekitTaggedItem primaryAccessory = getPrimaryAccessory(taggedItem, taggedItem.getServiceType(),
                    itemRegistry).orElse(null);
            Map<String, Item> characteristicItems = getCharacteristicTypeItemMap(taggedItem);

            if (primaryAccessory != null) {
                Accessory accessory = new GenericAccessory(eventManager, homekitFactories);
                accessory.assignToServer(server);
                Optional<Service> primaryService = createPrimaryService(serviceFactory, primaryAccessory, accessory,
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
     * @param serviceFactory The factory to create the service
     * @param primaryAccessory The primary accessory item
     * @param accessory The accessory to add the service to
     * @param taggedItem The tagged item
     * @return The created service, or empty if creation failed
     */
    @SuppressWarnings("null")
    private Optional<Service> createPrimaryService(HomekitFactory serviceFactory, HomekitTaggedItem primaryAccessory,
            Accessory accessory, HomekitTaggedItem taggedItem) {
        logger.trace("{}Entering createPrimaryService", LOG_TRACE);

        try {
            logger.debug("{}Creating primary service for item: {}", LOG_DEBUG, taggedItem.getName());
            Service primaryService = serviceFactory.createService(primaryAccessory.getServiceType(), accessory,
                    accessory.getNextAvailableInstanceId(), true,
                    "Primary Service for " + taggedItem.getItem().getName());
            logger.debug("{}Successfully created primary service for item: {}", LOG_DEBUG, taggedItem.getName());
            logger.trace("{}Exiting createPrimaryService successfully", LOG_TRACE);
            return Optional.ofNullable(primaryService);
        } catch (org.openhab.io.homekit.exception.HomekitFactoryException e) {
            logger.warn(SERVICE_CREATION_FAILED, primaryAccessory.getServiceType(), taggedItem.getName());
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
    private void addCharacteristics(Service service, Map<String, Item> characteristicItems, Accessory accessory) {
        for (Map.Entry<String, Item> entry : characteristicItems.entrySet()) {
            @Nullable
            String characteristicType = entry.getKey();
            @Nullable
            Item item = entry.getValue();

            Optional<HomekitFactory> compatibleCharacteristicFactory = findCompatibleCharacteristicFactory(
                    characteristicType);
            if (shouldAddCharacteristic(service, characteristicType, compatibleCharacteristicFactory)) {
                addCharacteristicToService(service, characteristicType, item, accessory,
                        compatibleCharacteristicFactory.get());
            }
        }
    }

    /**
     * Determines if a characteristic should be added to a service.
     * 
     * @param primaryService The service to check
     * @param characteristicType The type of characteristic
     * @param compatibleFactory Optional containing the compatible factory
     * @return true if the characteristic should be added, false otherwise
     */
    private boolean shouldAddCharacteristic(Service primaryService, String characteristicType,
            Optional<HomekitFactory> compatibleFactory) {
        return primaryService.isExtensible() && !primaryService.getCharacteristics().stream()
                .anyMatch(c -> c.getInstanceType().equals(characteristicType)) && compatibleFactory.isPresent();
    }

    /**
     * Adds a characteristic to a service and sets up its listener.
     * 
     * @param service The service to add the characteristic to
     * @param characteristicType The type of characteristic to add
     * @param item The item associated with the characteristic
     * @param accessory The accessory containing the service
     * @param characteristicFactory The factory to create the characteristic
     */
    private void addCharacteristicToService(Service service, String characteristicType, Item item, Accessory accessory,
            HomekitFactory characteristicFactory) {
        try {
            logger.debug("{}Adding characteristic {} to service", LOG_DEBUG, characteristicType);
            Characteristic<?> characteristic = characteristicFactory.createCharacteristic(characteristicType, service,
                    accessory.getNextAvailableInstanceId());
            if (characteristic != null) {
                service.addCharacteristic(characteristic);
                addCharacteristic(item.getName(), characteristic);
                subscribeToEvents(characteristic, item);
            }
            logger.debug("{}Successfully added characteristic {} to service", LOG_DEBUG, characteristicType);
        } catch (org.openhab.io.homekit.exception.HomekitFactoryException e) {
            logger.warn(CHARACTERISTIC_CREATION_FAILED, characteristicType, item.getName());
        }
    }

    /**
     * Sets up the listener for a characteristic.
     * 
     * @param characteristic The characteristic to set up the listener for
     * @param item The item associated with the characteristic
     */
    private void subscribeToEvents(Characteristic<?> characteristic, Item item) {
        if (!(characteristic instanceof GenericCharacteristic<?> genericCharacteristic)) {
            return;
        }
        eventManager.subscribe(HomekitEventType.CHARACTERISTIC_STATE_CHANGED, genericCharacteristic.getUID().toString(),subscriberUID,
                event -> handleCharacteristicStateChangedEvent(event, item));
    }

    private void handleCharacteristicStateChangedEvent(HomekitEvent event, Item item) {
        if (event.getType() != HomekitEventType.CHARACTERISTIC_STATE_CHANGED) {
            return;
        }
        if (!(event instanceof CharacteristicEvent characteristicEvent)) {
            return;
        }
        Characteristic<?> eventCharacteristic = characteristicEvent.getCharacteristic().get();
        State state = eventCharacteristic.toState(characteristicEvent.getNewValue());
        if (state != null) {
            eventPublisher.post(ItemEventFactory.createStateEvent(item.getName(), state));
        }
    }

    /**
     * Updates the value of a characteristic based on the new state.
     * This method is synchronized to ensure thread-safe updates.
     * 
     * @param characteristic The characteristic to update
     * @param state The new state value
     */
    private void updateCharacteristicValue(Characteristic<?> characteristic, State state) {
        if (characteristic == null || state == null) {
            return;
        }

        try {
            if (characteristic instanceof GenericCharacteristic) {
                @SuppressWarnings("unchecked")
                GenericCharacteristic<Object> genericCharacteristic = (GenericCharacteristic<Object>) characteristic;
                Object value = genericCharacteristic.toValue(state);
                if (value != null) {
                    synchronized (characteristicLock) {
                        genericCharacteristic.setValue(value);
                    }
                    logger.debug(DEBUG_UPDATING_CHARACTERISTIC, characteristic.getInstanceType(),
                            characteristic.getService().getAccessory().getUID(), state);
                }
            }
        } catch (Exception e) {
            logger.error(ERROR_UPDATING_CHARACTERISTIC, characteristic.getInstanceType(), e.getMessage(), e);
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
            return homekitFactories.stream().filter(Objects::nonNull)
                    .map(factory -> factory.getTagFromServiceType(serviceType)).filter(Objects::nonNull)
                    .flatMap(tag -> groupItem.getMembers().stream().filter(item -> item.hasTag(tag))
                            .map(item -> new HomekitTaggedItem(item, itemRegistry, metadataRegistry, homekitFactories)))
                    .findFirst();
        } else if (serviceType.equals(taggedItem.getServiceType())) {
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
                String type = getHomekitTags(item).stream().map(tag -> {
                    return homekitFactories.stream().map(factory -> factory.getCharacteristicTypeFromTag(tag))
                            .filter(Objects::nonNull).findFirst().orElse(null);
                }).filter(Objects::nonNull).findFirst().orElse(null);
                if (type != null) {
                    if (characteristicItems.containsKey(type)) {
                        logger.warn("incorrect configuration for {} detected: {} and {} are tagged as {}, skipping {}",
                                taggedItem.getItem().getUID(),
                                java.util.Objects.requireNonNull(characteristicItems.get(type)).getUID(), item.getUID(),
                                type, item.getUID());
                    } else {
                        characteristicItems.put(type, item);
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
     * If the item is tagged for HomeKit integration, creates a new accessory for it.
     * 
     * @param item The item that was added to the registry
     */
    @Override
    public void added(Item item) {
        HomekitTaggedItem taggedItem = new HomekitTaggedItem(item, itemRegistry, metadataRegistry, homekitFactories);
        if (taggedItem.isTagged()) {
            createAccessoryForItem(taggedItem);
        }
    }

    /**
     * Handles the removal of an item from the registry.
     * Removes the associated HomeKit accessory if it exists.
     * 
     * @param item The item that was removed from the registry
     */
    @Override
    public void removed(Item item) {
        removeAccessoryForItem(item);
    }

    /**
     * Handles the update of an existing item in the registry.
     * Removes the old accessory and creates a new one if the updated item is tagged for HomeKit integration.
     * 
     * @param oldItem The previous version of the item
     * @param item The updated version of the item
     */
    @Override
    public void updated(Item oldItem, Item item) {
        removeAccessoryForItem(oldItem);
        HomekitTaggedItem taggedItem = new HomekitTaggedItem(item, itemRegistry, metadataRegistry, homekitFactories);
        if (taggedItem.isTagged()) {
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
                .map(item -> new HomekitTaggedItem(item, itemRegistry, metadataRegistry, homekitFactories))
                .filter(HomekitTaggedItem::isTagged).forEach(this::createAccessoryForItem);
    }

    // ========== State Change Listener Methods ==========
    /**
     * Handles state changes for items.
     * Updates the corresponding HomeKit characteristic values when an item's state changes.
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

        @Nullable
        Collection<Characteristic<?>> characteristics = characteristicMap.get(item.getName());
        if (characteristics != null) {
            characteristics.forEach(c -> {
                try {
                    updateCharacteristicValue(c, newState);
                } catch (Exception e) {
                    logger.error(ERROR_UPDATING_CHARACTERISTIC, c.getInstanceType(), e.getMessage(), e);
                }
            });
        }
    }

    /**
     * Handles state updates for items.
     * Updates the corresponding HomeKit characteristic values when an item's state is updated.
     * 
     * @param item The item whose state was updated
     * @param state The new state of the item
     */
    @Override
    public void stateUpdated(Item item, State state) {
        Optional.ofNullable(characteristicMap.get(item.getName()))
                .ifPresent(characteristics -> characteristics.forEach(c -> updateCharacteristicValue(c, state)));
    }

    private Collection<String> getHomekitTags(Item item) {
        MetadataKey key = new MetadataKey("homekit", item.getName());
        Metadata metadata = metadataRegistry.get(key);
        return metadata != null ? Arrays.asList(metadata.getValue().split(",")) : Collections.emptyList();
    }
}
