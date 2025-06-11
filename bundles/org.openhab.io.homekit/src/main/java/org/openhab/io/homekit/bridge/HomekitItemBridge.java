/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.openhab.io.homekit.bridge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
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
import org.openhab.core.library.types.DecimalType;
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
import org.openhab.io.homekit.config.HomekitConfigurationManager;
import org.openhab.io.homekit.core.accessory.HomekitAccessoryRegistryImpl;
import org.openhab.io.homekit.core.characteristic.AbstractHomekitCharacteristic;
import org.openhab.io.homekit.core.event.HomekitPeerGroupUIDImpl;
import org.openhab.io.homekit.event.core.HomekitEventMetadata;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.event.model.characteristic.HomekitCharacteristicChangedEvent;
import org.openhab.io.homekit.event.model.characteristic.HomekitCharacteristicUpdateEvent;
import org.openhab.io.homekit.util.HomekitUID;
import org.openhab.io.homekit.util.ItemUID;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link HomekitItemBridge} manages the integration between openHAB items
 * and HomeKit accessories.
 * It handles the complete lifecycle of HomeKit accessories, including creation,
 * updates, and removal.
 *
 * This class implements both {@link ItemRegistryChangeListener} and
 * {@link StateChangeListener} to handle:
 * - Item lifecycle events (addition, removal, updates)
 * - State changes for HomeKit-enabled items
 * - {@link HomekitCharacteristic} value updates
 * - {@link HomekitAccessory} registration and cleanup
 *
 * The bridge maintains thread-safe collections for:
 * - Accessories ({@link #accessoryMap})
 * - Characteristics ({@link #characteristicMap})
 * - Factories ({@link #homekitFactories})
 *
 * The class integrates with:
 * - {@link org.openhab.core.items.ItemRegistry} for item management
 * - {@link org.openhab.core.events.EventPublisher} for event handling
 * - {@link org.openhab.core.items.MetadataRegistry} for metadata management
 * - {@link org.openhab.io.homekit.api.registry.HomekitAccessoryServerRegistry}
 * for server management
 * - {@link org.openhab.io.homekit.api.factory.HomekitAccessoryFactory} for
 * accessory creation
 * - {@link org.openhab.io.homekit.api.factory.HomekitServiceFactory} for
 * service creation
 * - {@link org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory} for
 * characteristic creation
 * - {@link org.openhab.io.homekit.config.HomekitConfigurationManager} for
 * configuration management
 * - {@link org.openhab.io.homekit.event.manager.HomekitEventManager} for event
 * management
 * - {@link org.openhab.io.homekit.core.accessory.HomekitAccessoryRegistryImpl}
 * for accessory registry
 *
 * Key implementation details:
 * - Thread-safe collections for accessories and characteristics
 * - Event correlation to prevent feedback loops
 * - Orphaned accessory management
 * - Statistics collection for performance monitoring
 * - Bidirectional state/command conversion
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0.0
 */
@Component(service = HomekitItemBridge.class, immediate = true)
@NonNullByDefault
public class HomekitItemBridge implements ItemRegistryChangeListener, StateChangeListener {

    private static final Logger logger = LoggerFactory.getLogger(HomekitItemBridge.class);

    // ========== Configuration Constants ==========
    private static final boolean ENABLE_EXIT_EVENT_STATISTICS = true;
    private static final int MAX_STATISTICS_ENTRIES = 1000;
    private static final int STATISTICS_REPORT_INTERVAL_SECONDS = 60;
    private static final String CONFIG_ORPHAN_ENABLED = "orphanEnabled";
    private static final String YAML_FILE_NAME = "homekit-2.x-items.yaml";

    // ========== Thread Safety ==========
    private final Object accessoryLock = new Object();
    private final Object characteristicLock = new Object();

    // ========== Service Dependencies ==========
    private final ItemRegistry itemRegistry;
    private final EventPublisher eventPublisher;
    private final HomekitAccessoryRegistryImpl accessoryRegistry;
    private final MetadataRegistry metadataRegistry;
    private final HomekitAccessoryServerRegistry accessoryServerRegistry;
    private final HomekitEventManager eventManager;
    private final HomekitServiceFactory serviceFactory;
    private final HomekitCharacteristicFactory characteristicFactory;
    private final HomekitAccessoryFactory accessoryFactory;
    private final HomekitItemConfigParser configParser;
    private final HomekitConfigurationManager configManager;

    // ========== State Management ==========
    private final Map<String, Collection<HomekitCharacteristic<?>>> characteristicMap = new ConcurrentHashMap<>();
    private final Map<String, HomekitAccessory> accessoryMap = new ConcurrentHashMap<>();
    private final HomekitUID bridgeUID = new HomekitUID("bridge");
    private final Set<HomekitUID> peerGroup;
    private final Map<String, ExitEvent> exitEvents;
    private final ExitEventStatisticsCollector statisticsCollector;
    private boolean orphanEnabled = true; // Default to true for backward compatibility

    private static final String LOG_PREFIX = "Homekit Bridge: ";
    private static final String LOG_ACCESSORY = LOG_PREFIX + "Accessory - ";
    private static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    private static final String LOG_WARN = LOG_PREFIX + "Warning - ";
    private static final String LOG_DEBUG = LOG_PREFIX + "Debug - ";
    private static final String LOG_TRACE = LOG_PREFIX + "Trace - ";

    /**
     * Activates the bridge component and initializes necessary resources.
     *
     * This method initializes the bridge with all required services and
     * configurations.
     * It sets up event listeners, loads existing items, and configures statistics
     * collection.
     *
     * Key implementation details:
     * - Initializes service dependencies
     * - Loads orphan configuration
     * - Sets up item registry listener
     * - Initializes existing HomeKit tagged items
     * - Configures event statistics collection
     *
     * @param itemRegistry The {@link ItemRegistry} service
     * @param eventPublisher The {@link EventPublisher} service
     * @param accessoryRegistry The {@link HomekitAccessoryRegistryImpl}
     *            service
     * @param metadataRegistry The {@link MetadataRegistry} service
     * @param accessoryServerRegistry The {@link HomekitAccessoryServerRegistry}
     *            service
     * @param eventManager The {@link HomekitEventManager} service
     * @param accessoryFactory The {@link HomekitAccessoryFactory} service
     * @param serviceFactory The {@link HomekitServiceFactory} service
     * @param characteristicFactory The {@link HomekitCharacteristicFactory}
     *            service
     * @param configManager The {@link HomekitConfigurationManager}
     *            service
     * @param properties The component properties
     * @since 1.0.0
     */
    @Activate
    public HomekitItemBridge(@Reference ItemRegistry itemRegistry, @Reference EventPublisher eventPublisher,
            @Reference HomekitAccessoryRegistryImpl accessoryRegistry, @Reference MetadataRegistry metadataRegistry,
            @Reference HomekitAccessoryServerRegistry accessoryServerRegistry,
            @Reference HomekitEventManager eventManager, @Reference HomekitAccessoryFactory accessoryFactory,
            @Reference HomekitServiceFactory serviceFactory,
            @Reference HomekitCharacteristicFactory characteristicFactory,
            @Reference HomekitConfigurationManager configManager, Map<String, Object> properties) {
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

        // Load orphan configuration
        Optional.ofNullable(properties.get(CONFIG_ORPHAN_ENABLED)).ifPresent(orphanConfig -> {
            @SuppressWarnings("null") // orphanConfig is non-null inside ifPresent
            String configValue = orphanConfig.toString();
            this.orphanEnabled = Boolean.parseBoolean(configValue);
            logger.info("{}Orphan functionality is {}", LOG_PREFIX, orphanEnabled ? "enabled" : "disabled");
        });

        itemRegistry.addRegistryChangeListener(this);

        // Initialize existing Homekit tagged items
        for (Item item : itemRegistry.getItems()) {
            HomekitTaggedItem taggedItem = new HomekitTaggedItem(item, itemRegistry, metadataRegistry, serviceFactory,
                    characteristicFactory);
            if (taggedItem.isTagged()) {
                createAccessoryForItem(taggedItem);
            }
        }

        this.peerGroup = Set.of(bridgeUID, new HomekitPeerGroupUIDImpl("openhab"),
                new HomekitPeerGroupUIDImpl("homekit"));
        this.exitEvents = new ConcurrentHashMap<>();
        this.statisticsCollector = new ExitEventStatisticsCollector();

        if (ENABLE_EXIT_EVENT_STATISTICS) {
            statisticsCollector.start();
        }
    }

    /**
     * Deactivates the bridge component and cleans up resources.
     *
     * This method performs cleanup operations when the bridge is deactivated.
     * It removes event listeners, stops statistics collection, and cleans up
     * accessories.
     *
     * Key implementation details:
     * - Removes item registry listener
     * - Stops statistics collection
     * - Cleans up accessories and characteristics
     * - Releases resources
     *
     * @since 1.0.0
     */
    @Deactivate
    protected void deactivate() {
        logger.debug("{}Deactivating Homekit bridge", LOG_PREFIX);
        itemRegistry.removeRegistryChangeListener(this);
        if (ENABLE_EXIT_EVENT_STATISTICS) {
            statisticsCollector.stop();
        }
        cleanup();
    }

    /**
     * Cleans up all resources associated with the bridge.
     *
     * This method removes all accessories and characteristics, ensuring proper
     * cleanup
     * of all HomeKit-related resources.
     *
     * Key implementation details:
     * - Removes all accessories from registry
     * - Cleans up characteristic mappings
     * - Handles cleanup errors gracefully
     *
     * @since 1.0.0
     */
    private void cleanup() {
        logger.debug("{}Cleaning up Homekit bridge resources", LOG_PREFIX);
        synchronized (accessoryLock) {
            for (HomekitAccessory accessory : accessoryMap.values()) {
                try {
                    accessoryRegistry.remove(accessory.getUID());
                    logger.debug("{}Removed accessory {}", LOG_ACCESSORY, accessory.getUID());
                } catch (Exception e) {
                    logger.warn("{}Failed to remove accessory {}: {}", LOG_WARN, accessory.getUID(), e.getMessage());
                }
            }
            accessoryMap.clear();
        }

        synchronized (characteristicLock) {
            characteristicMap.clear();
        }
        logger.debug("{}Cleanup completed", LOG_PREFIX);
    }

    /**
     * Adds a characteristic to the map for an item.
     *
     * This method is synchronized to ensure thread-safe characteristic management.
     *
     * @param itemName The name of the item
     * @param characteristic The {@link HomekitCharacteristic} to add
     * @since 1.0.0
     */
    private void addCharacteristic(String itemName, HomekitCharacteristic<?> characteristic) {
        synchronized (characteristicLock) {
            Collection<HomekitCharacteristic<?>> characteristics = characteristicMap.computeIfAbsent(itemName,
                    k -> new ArrayList<>());
            if (characteristics != null) {
                characteristics.add(characteristic);
            }
        }
    }

    /**
     * Removes a characteristic from the map for an item.
     *
     * This method is synchronized to ensure thread-safe characteristic management.
     *
     * @param itemName The name of the item
     * @param characteristic The {@link HomekitCharacteristic} to remove
     * @since 1.0.0
     */
    private void removeCharacteristic(String itemName, HomekitCharacteristic<?> characteristic) {
        synchronized (characteristicLock) {
            Optional.ofNullable(characteristicMap.get(itemName)).ifPresent(characteristics -> {
                characteristics.remove(characteristic);
                if (characteristics.isEmpty()) {
                    characteristicMap.remove(itemName);
                }
            });
        }
    }

    /**
     * Creates a HomeKit accessory for an item.
     *
     * This method processes the item's configuration and creates the appropriate
     * HomeKit accessory with its services and characteristics.
     *
     * @param taggedItem The {@link HomekitTaggedItem} to create an accessory for
     * @since 1.0.0
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

            Optional<HomekitAccessoryServer> serverOpt = accessoryServerRegistry.getAvailableBridgeAccessoryServer();
            if (serverOpt.isPresent()) {
                @SuppressWarnings("null") // get() is safe after isPresent() check
                HomekitAccessoryServer server = serverOpt.get();
                logger.debug("{}Found available server {} for item {}", LOG_PREFIX, server.getUID(),
                        taggedItem.getName());
                createAndRegisterAccessory(taggedItem, server);
            } else {
                logger.warn("{}No available bridge accessory server found for item {}", LOG_WARN, taggedItem.getName());
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
     * Creates and registers a HomeKit accessory for an item.
     *
     * This method creates the accessory and registers it with the appropriate
     * server.
     *
     * @param taggedItem The {@link HomekitTaggedItem} to create an accessory for
     * @param server The {@link HomekitAccessoryServer} to register the
     *            accessory with
     * @since 1.0.0
     */
    private void createAndRegisterAccessory(HomekitTaggedItem taggedItem, HomekitAccessoryServer server) {
        logger.trace("{}Entering createAndRegisterAccessory", LOG_TRACE);

        try {
            logger.debug("{}Creating accessory for item: {}", LOG_DEBUG, taggedItem.getName());
            Optional<HomekitAccessory> accessory = createAccessory(taggedItem, server);
            if (accessory.isPresent()) {
                synchronized (accessoryLock) {
                    @SuppressWarnings("null") // get() is safe after isPresent() check
                    HomekitAccessory validAccessory = accessory.get();
                    registerAccessory(taggedItem, validAccessory);
                }
                logger.debug("{}Successfully created Homekit accessory for item {}", LOG_ACCESSORY,
                        taggedItem.getName());
            }
            logger.debug("{}Successfully processed item with factory", LOG_DEBUG);
            logger.trace("{}Exiting createAndRegisterAccessory successfully", LOG_TRACE);
        } catch (Exception e) {
            logger.error("{}Error creating Homekit accessory for item {}: {}", LOG_ERROR, taggedItem.getName(),
                    e.getMessage(), e);
            logger.trace("{}Exiting createAndRegisterAccessory with error", LOG_TRACE);
        }
    }

    /**
     * Registers the created accessory with the registry.
     *
     * This method is synchronized to ensure thread-safe registration.
     *
     * @param taggedItem The {@link HomekitTaggedItem} associated with the accessory
     * @param accessory The {@link HomekitAccessory} to register
     * @since 1.0.0
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
            logger.error("{}Error creating Homekit accessory for item {}: {}", LOG_ERROR, taggedItem.getName(),
                    e.getMessage(), e);
        }
    }

    /**
     * Removes an accessory for the given item.
     *
     * This method is synchronized to ensure thread-safe removal.
     *
     * @param item The {@link Item} whose accessory should be removed
     * @since 1.0.0
     */
    private void removeAccessoryForItem(Item item) {
        logger.debug("{}Removing Homekit accessory for item {}", LOG_ACCESSORY, item.getName());

        synchronized (accessoryLock) {
            Optional.ofNullable(accessoryMap.remove(item.getName())).ifPresent(accessory -> {
                try {
                    accessoryRegistry.remove(accessory.getUID());
                    accessory.getServices().forEach(service -> {
                        service.getCharacteristics().forEach(characteristic -> {
                            synchronized (characteristicLock) {
                                removeCharacteristic(item.getName(), characteristic);
                            }
                        });
                    });
                    logger.debug("{}Successfully removed Homekit accessory for item {}", LOG_ACCESSORY, item.getName());
                } catch (Exception e) {
                    logger.error("{}Error removing accessory {}: {}", LOG_ERROR, accessory.getUID(), e.getMessage(), e);
                }
            });
        }
    }

    /**
     * Creates a HomeKit accessory for an item.
     *
     * This method processes the item's configuration and creates the appropriate
     * HomeKit accessory with its services and characteristics.
     *
     * @param taggedItem The {@link HomekitTaggedItem} to create an accessory for
     * @param server The {@link HomekitAccessoryServer} to register the
     *            accessory with
     * @return Optional containing the created {@link HomekitAccessory}, or empty if
     *         creation failed
     * @since 1.0.0
     */
    private Optional<HomekitAccessory> createAccessory(HomekitTaggedItem taggedItem, HomekitAccessoryServer server) {
        logger.trace("{}Entering createAccessory", LOG_TRACE);

        try {
            logger.debug("{}Creating accessory for item: {}", LOG_DEBUG, taggedItem.getName());
            String serviceTag = taggedItem.getServiceTag();
            if (serviceTag == null) {
                logger.warn("{}Service tag is null for item: {}", LOG_ERROR, taggedItem.getName());
                return Optional.empty();
            }
            Optional<HomekitTaggedItem> primaryAccessory = getPrimaryAccessory(taggedItem, serviceTag, itemRegistry);
            Map<String, Item> characteristicItems = getCharacteristicTypeItemMap(taggedItem);

            if (primaryAccessory.isPresent()) {
                HomekitAccessory accessory = accessoryFactory.createAccessoryFromTag("generic");
                accessory.assignToServer(server);
                @SuppressWarnings("null") // get() is safe after isPresent() check
                HomekitTaggedItem validPrimaryAccessory = primaryAccessory.get();
                Optional<HomekitService> primaryService = createPrimaryService(validPrimaryAccessory, accessory,
                        taggedItem);
                if (primaryService.isPresent()) {
                    @SuppressWarnings("null") // get() is safe after isPresent() check
                    HomekitService validPrimaryService = primaryService.get();
                    accessory.addService(validPrimaryService);
                    addCharacteristics(validPrimaryService, characteristicItems, accessory);
                    return Optional.of(accessory);
                }
            }
            logger.debug("{}Successfully processed item with factory", LOG_DEBUG);
            logger.trace("{}Exiting createAccessory successfully", LOG_TRACE);
            return Optional.empty();
        } catch (Exception e) {
            logger.warn("{}Error creating Homekit accessory for item {}: {}", LOG_ERROR, taggedItem.getName(),
                    e.getMessage());
            logger.trace("{}Exiting createAccessory with error", LOG_TRACE);
            return Optional.empty();
        }
    }

    /**
     * Creates the primary service for an accessory.
     *
     * This method creates the main service for the accessory based on its
     * configuration.
     *
     * @param primaryAccessoryItem The primary {@link HomekitTaggedItem}
     * @param accessory The {@link HomekitAccessory} to create the
     *            service for
     * @param taggedItem The {@link HomekitTaggedItem} to create the
     *            service for
     * @return Optional containing the created {@link HomekitService}, or empty if
     *         creation failed
     * @since 1.0.0
     */
    private Optional<HomekitService> createPrimaryService(HomekitTaggedItem primaryAccessoryItem,
            HomekitAccessory accessory, HomekitTaggedItem taggedItem) {
        logger.trace("{}Entering createPrimaryService", LOG_TRACE);

        try {
            logger.debug("{}Creating primary service for item: {}", LOG_DEBUG, taggedItem.getName());
            String serviceTag = primaryAccessoryItem.getServiceTag();
            if (serviceTag == null) {
                logger.warn("{}Service tag is null for item: {}", LOG_ERROR, taggedItem.getName());
                return Optional.empty();
            }
            HomekitService primaryService = serviceFactory.createServiceFromTag(serviceTag, accessory);
            primaryService.withInstanceId(accessory.getNextAvailableInstanceId())
                    .withName("Primary HomekitService for " + taggedItem.getItem().getName()).withExtensible(true);

            logger.debug("{}Successfully created primary service for item: {}", LOG_DEBUG, taggedItem.getName());
            logger.trace("{}Exiting createPrimaryService successfully", LOG_TRACE);
            return Optional.ofNullable(primaryService);
        } catch (Exception e) {
            logger.warn("{}Failed to create service {} for item {}", LOG_ERROR, primaryAccessoryItem.getServiceTag(),
                    taggedItem.getName());
            logger.trace("{}Exiting createPrimaryService with error", LOG_TRACE);
            return Optional.empty();
        }
    }

    /**
     * Adds characteristics to a service.
     *
     * This method adds all configured characteristics to the given service.
     *
     * @param service The {@link HomekitService} to add characteristics
     *            to
     * @param characteristicItems Map of characteristic types to items
     * @param accessory The {@link HomekitAccessory} the service belongs
     *            to
     * @since 1.0.0
     */
    private void addCharacteristics(HomekitService service, Map<String, Item> characteristicItems,
            HomekitAccessory accessory) {
        for (Map.Entry<String, Item> entry : characteristicItems.entrySet()) {
            @SuppressWarnings("null") // entrySet().getKey() and getValue() guaranteed non-null for valid entries
            String characteristicTag = entry.getKey();
            @SuppressWarnings("null") // entrySet().getKey() and getValue() guaranteed non-null for valid entries
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
            service.addCharacteristic(characteristic);
            addCharacteristic(item.getName(), characteristic);
            subscribeToEvents(characteristic, item);
            logger.debug("{}Successfully added characteristic {} to service", LOG_DEBUG, characteristicTag);
        } catch (Exception e) {
            logger.warn("{}Error creating Homekit characteristic for item {}: {}", LOG_ERROR, item.getName(),
                    e.getMessage());
        }
    }

    /**
     * Subscribes to events for a characteristic.
     *
     * This method sets up event subscriptions for characteristic value changes.
     *
     * @param characteristic The {@link HomekitCharacteristic} to subscribe to
     * @param item The {@link Item} associated with the characteristic
     * @since 1.0.0
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

        var characteristicOpt = ((HomekitCharacteristicChangedEvent) event).getCharacteristic();
        var newValueOpt = ((HomekitCharacteristicChangedEvent) event).getNewValue();

        if (characteristicOpt.isPresent() && newValueOpt.isPresent()) {
            @SuppressWarnings("null") // get() is safe after isPresent() check
            HomekitCharacteristic<?> eventCharacteristic = characteristicOpt.get();
            @SuppressWarnings("null") // get() is safe after isPresent() check
            JsonValue newValue = newValueOpt.get();
            State newState = eventCharacteristic.toState(newValue);
            // Store the exit event
            exitEvents.put(item.getName(), new ExitEvent(newState, event.getMetadata()));

            // Post the state change to OpenHAB
            eventPublisher.post(ItemEventFactory.createStateEvent(item.getName(), newState));
        }
    }

    /**
     * Gets the primary accessory for a tagged item.
     *
     * @param taggedItem The {@link HomekitTaggedItem} to get the primary
     *            accessory for
     * @param serviceType The type of service
     * @param itemRegistry The {@link ItemRegistry} to use
     * @return Optional containing the primary {@link HomekitTaggedItem}, or empty
     *         if not found
     * @since 1.0.0
     */
    private Optional<HomekitTaggedItem> getPrimaryAccessory(HomekitTaggedItem taggedItem, String serviceType,
            ItemRegistry itemRegistry) {

        logger.debug("{}: isGroup? {}, isMember? {}", taggedItem.getName(), taggedItem.isGroup(),
                taggedItem.isMemberOfAccessoryGroup());

        if (taggedItem.isGroup()) {
            GroupItem groupItem = (GroupItem) taggedItem.getItem();
            return groupItem.getMembers().stream().filter(item -> item.hasTag(serviceType))
                    .map(item -> new HomekitTaggedItem(item, itemRegistry, metadataRegistry, serviceFactory,
                            characteristicFactory))
                    .findFirst();
        } else if (Objects.equals(serviceType, taggedItem.getServiceTag())) {
            return Optional.of(taggedItem);
        }
        return Optional.empty();
    }

    /**
     * Gets a map of characteristic types to items.
     *
     * @param taggedItem The {@link HomekitTaggedItem} to get the map for
     * @return Map of characteristic types to items
     * @since 1.0.0
     */
    private Map<String, Item> getCharacteristicTypeItemMap(HomekitTaggedItem taggedItem) {
        if (taggedItem.isGroup()) {
            GroupItem groupItem = (GroupItem) taggedItem.getItem();
            Map<String, Item> characteristicItems = new HashMap<>();
            groupItem.getMembers().forEach(item -> {
                var tagOpt = getHomekitTags(item).stream().filter(aTag -> characteristicFactory.supportsTag(aTag))
                        .findFirst();
                if (tagOpt.isPresent()) {
                    @SuppressWarnings("null")
                    String tag = tagOpt.get();
                    if (characteristicItems.containsKey(tag)) {
                        Item existingItem = characteristicItems.get(tag);
                        if (existingItem != null) {
                            logger.warn(
                                    "incorrect configuration for {} detected: {} and {} are tagged as {}, skipping {}",
                                    taggedItem.getItem().getUID(), existingItem.getUID(), item.getUID(), tag,
                                    item.getUID());
                        }
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
     * Handles item addition events.
     *
     * This method processes events when items are added to the registry.
     *
     * @param item The {@link Item} that was added
     * @since 1.0.0
     */
    @Override
    public void added(Item item) {
        HomekitTaggedItem taggedItem = new HomekitTaggedItem(item, itemRegistry, metadataRegistry, serviceFactory,
                characteristicFactory);
        if (taggedItem.isTagged()) {
            if (orphanEnabled) {
                // Check if this is a restoration of an orphaned accessory
                @SuppressWarnings("null") // accessoryMap.get() can return null, checked below
                HomekitAccessory existingAccessory = accessoryMap.get(item.getName());
                if (existingAccessory != null && existingAccessory.isOrphaned()) {
                    if (restoreOrphanedAccessory(item, existingAccessory)) {
                        logger.info("{}Successfully restored orphaned accessory for item {}", LOG_PREFIX,
                                item.getName());
                        return;
                    }
                }
            }
            // Get configuration with proper priority
            Map<String, Object> config = getItemConfiguration(item);
            configManager.updateConfiguration(new ItemUID("openhab:item:" + item.getName()),
                    HomekitConfigurationManager.ConfigurationType.ITEM, config, YAML_FILE_NAME);
            createAccessoryForItem(taggedItem);
        }
    }

    /**
     * Restores an orphaned accessory.
     *
     * This method attempts to restore an orphaned accessory when its item becomes
     * available again.
     *
     * @param item The {@link Item} to restore the accessory for
     * @param orphanedAccessory The orphaned {@link HomekitAccessory}
     * @return true if the accessory was restored, false otherwise
     * @since 1.0.0
     */
    private boolean restoreOrphanedAccessory(Item item, HomekitAccessory orphanedAccessory) {
        try {
            // Remove orphaned flag
            orphanedAccessory.setOrphaned(false);

            // Update configuration
            Optional<Map<String, Object>> configOpt = configManager.getConfiguration(
                    new ItemUID("openhab:item:" + item.getName()), HomekitConfigurationManager.ConfigurationType.ITEM);
            if (configOpt.isPresent()) {
                Map<String, Object> config = new HashMap<>(configOpt.get());
                config.remove("orphaned");
                configManager.updateConfiguration(new ItemUID("openhab:item:" + item.getName()),
                        HomekitConfigurationManager.ConfigurationType.ITEM, config, YAML_FILE_NAME);
            }

            // Re-subscribe to item state changes
            orphanedAccessory.getServices().forEach(service -> {
                service.getCharacteristics().forEach(characteristic -> {
                    if (characteristic instanceof AbstractHomekitCharacteristic<?> genericCharacteristic) {
                        subscribeToEvents(genericCharacteristic, item);
                    }
                });
            });

            logger.info("{}Successfully restored orphaned accessory for item {}", LOG_PREFIX, item.getName());
            return true;
        } catch (Exception e) {
            logger.error("{}Failed to restore orphaned accessory for item {}: {}", LOG_PREFIX, item.getName(),
                    e.getMessage(), e);
            return false;
        }
    }

    /**
     * Handles item removal events.
     *
     * This method processes events when items are removed from the registry.
     *
     * @param item The {@link Item} that was removed
     * @since 1.0.0
     */
    @Override
    public void removed(Item item) {
        @SuppressWarnings("null") // accessoryMap.get() can return null, checked below
        HomekitAccessory accessory = accessoryMap.get(item.getName());
        if (accessory != null) {
            if (orphanEnabled) {
                // Mark the accessory as orphaned but keep it in the registry
                logger.info("{}Item {} was removed but keeping its HomeKit accessory to prevent controller deletion",
                        LOG_PREFIX, item.getName());
                accessory.setOrphaned(true);

                // Update configuration to reflect orphaned state
                try {
                    Map<String, Object> config = new HashMap<>();
                    config.put("orphaned", true);
                    configManager.updateConfiguration(new ItemUID("openhab:item:" + item.getName()),
                            HomekitConfigurationManager.ConfigurationType.ITEM, config, YAML_FILE_NAME);
                } catch (Exception e) {
                    logger.error("{}Failed to update configuration for orphaned item {}: {}", LOG_PREFIX,
                            item.getName(), e.getMessage(), e);
                }
            } else {
                // Completely remove the accessory and its characteristics
                removeAccessoryForItem(item);
                configManager.removeConfiguration(new ItemUID("openhab:item:" + item.getName()),
                        HomekitConfigurationManager.ConfigurationType.ITEM);
            }
        }
    }

    /**
     * Handles item update events.
     *
     * This method processes events when items are updated in the registry.
     *
     * @param oldItem The old {@link Item}
     * @param item The new {@link Item}
     * @since 1.0.0
     */
    @Override
    public void updated(Item oldItem, Item item) {
        configManager.removeConfiguration(new ItemUID("openhab:item:" + oldItem.getName()),
                HomekitConfigurationManager.ConfigurationType.ITEM);
        removeAccessoryForItem(oldItem);
        HomekitTaggedItem taggedItem = new HomekitTaggedItem(item, itemRegistry, metadataRegistry, serviceFactory,
                characteristicFactory);
        if (taggedItem.isTagged()) {
            // Get configuration with proper priority
            Map<String, Object> config = getItemConfiguration(item);
            configManager.updateConfiguration(new ItemUID("openhab:item:" + item.getName()),
                    HomekitConfigurationManager.ConfigurationType.ITEM, config, YAML_FILE_NAME);
            createAccessoryForItem(taggedItem);
        }
    }

    /**
     * Handles all items changed events.
     *
     * This method processes events when all items in the registry are changed.
     *
     * @param oldItemNames The names of the old items
     * @since 1.0.0
     */
    @Override
    public void allItemsChanged(Collection<String> oldItemNames) {
        // Potential concurrent modification if items are added/removed during iteration
        oldItemNames.forEach(itemName -> {
            @SuppressWarnings("null") // itemRegistry.get() can return null, which is checked below
            Item item = itemRegistry.get(itemName);
            if (item != null) {
                removeAccessoryForItem(item);
            }
        });

        // Create new accessories for all tagged items
        itemRegistry.getItems().stream()
                .map(item -> new HomekitTaggedItem(item, itemRegistry, metadataRegistry, serviceFactory,
                        characteristicFactory))
                .filter(HomekitTaggedItem::isTagged).forEach(this::createAccessoryForItem);
    }

    // ========== State Change Listener Methods ==========
    /**
     * Gets the configuration for an item.
     *
     * @param item The {@link Item} to get the configuration for
     * @return Map of configuration properties
     * @since 1.0.0
     */
    private Map<String, Object> getItemConfiguration(Item item) {
        // First try to get configuration from YAML using fully qualified UID
        Optional<Map<String, Object>> yamlConfig = configManager.getConfiguration(
                new ItemUID("openhab:item:" + item.getName()), HomekitConfigurationManager.ConfigurationType.ITEM);

        if (yamlConfig.isPresent()) {
            @SuppressWarnings("null") // get() is safe after isPresent() check
            Map<String, Object> config = yamlConfig.get();
            return config;
        }

        // Fall back to filtered metadata configuration via configParser
        return configParser.getFilteredConfig(item);
    }

    /**
     * Handles item state change events.
     *
     * This method processes events when item states change.
     *
     * @param item The {@link Item} whose state changed
     * @param oldState The old {@link State}
     * @param newState The new {@link State}
     * @since 1.0.0
     */
    @Override
    public void stateChanged(Item item, State oldState, State newState) {
        // Item and newState are @NonNull parameters by annotation
        // The null check is redundant since we're using @NonNullByDefault at the class level

        // Get configuration with proper priority
        Map<String, Object> itemConfiguration = getItemConfiguration(item);

        // Clean up expired exit events
        exitEvents.entrySet().removeIf(entry -> {
            @SuppressWarnings("null") // Map.Entry.getValue() guaranteed non-null for valid entries
            ExitEvent event = entry.getValue();
            return event.isExpired();
        });

        Optional.ofNullable(characteristicMap.get(item.getName()))
                .ifPresent(characteristics -> characteristics.forEach(c -> {
                    try {
                        @SuppressWarnings("null") // exitEvents.get() can return null, which is handled by
                                                  // Optional.ofNullable
                        ExitEvent exitEvent = exitEvents.get(item.getName());

                        Optional.ofNullable(exitEvent).filter(e -> !e.isExpired()).ifPresent(e -> {
                            if (isStateEqual(e.getState(), newState)) {
                                logger.debug("Processing correlated state change for item: {}", item.getName());

                                if (ENABLE_EXIT_EVENT_STATISTICS) {
                                    statisticsCollector.recordEvent(System.currentTimeMillis() - e.getTimestamp());
                                }

                                HomekitEvent newEvent = new HomekitCharacteristicUpdateEvent((UID) bridgeUID,
                                        (UID) c.getUID(), c, c.toValueJson(e.getState()), c.toValueJson(newState),
                                        itemConfiguration, e.getMetadata());

                                eventManager.publishEvent(newEvent);
                                exitEvents.remove(item.getName());
                            }
                        });

                        // Handle uncorrelated state change
                        if (exitEvent == null) {
                            logger.debug("Processing new state change for item: {}", item.getName());
                            HomekitEvent newEvent = new HomekitCharacteristicUpdateEvent((UID) bridgeUID,
                                    (UID) c.getUID(), c, c.toValueJson(oldState), c.toValueJson(newState),
                                    itemConfiguration, new HomekitEventMetadata(bridgeUID, null, bridgeUID, peerGroup));
                            eventManager.publishEvent(newEvent);
                        }
                    } catch (Exception e) {
                        logger.error("{}Error updating characteristic {}: {}", LOG_ERROR, c.getType(), e.getMessage(),
                                e);
                    }
                }));
    }

    /**
     * Handles item state update events.
     *
     * This method processes events when item states are updated.
     *
     * @param item The {@link Item} whose state was updated
     * @param state The new {@link State}
     * @since 1.0.0
     */
    @Override
    public void stateUpdated(Item item, State state) {
        // Item and state are @NonNull parameters by annotation
        // The null check is redundant since we're using @NonNullByDefault at the class level

        // Get configuration with proper priority
        Map<String, Object> itemConfiguration = getItemConfiguration(item);

        Optional.ofNullable(characteristicMap.get(item.getName()))
                .ifPresent(characteristics -> characteristics.forEach(c -> {
                    try {
                        Optional.ofNullable(exitEvents.get(item.getName())).ifPresent(exitEvent -> {
                            HomekitEvent newEvent = new HomekitCharacteristicUpdateEvent(c,
                                    c.toValueJson(exitEvent.getState()), c.toValueJson(state), itemConfiguration,
                                    exitEvent.getMetadata());
                            eventManager.publishEvent(newEvent);
                        });
                    } catch (Exception e) {
                        logger.error("{}Error updating characteristic {}: {}", LOG_ERROR, c.getType(), e.getMessage(),
                                e);
                    }
                }));
    }

    private Collection<String> getHomekitTags(Item item) {
        MetadataKey key = new MetadataKey("homekit", item.getName());
        @SuppressWarnings("null") // metadataRegistry.get() can return null, which is checked below
        Metadata metadata = metadataRegistry.get(key);
        return metadata != null ? Arrays.asList(metadata.getValue().split(",")) : Collections.emptyList();
    }

    private boolean isStateEqual(State state1, State state2) {
        if (state1 == state2) {
            return true;
        }
        // States are @NonNull by annotation in method signature
        // This method is used in contexts where we've already verified non-null
        if (state1 instanceof DecimalType && state2 instanceof DecimalType) {
            return ((DecimalType) state1).doubleValue() == ((DecimalType) state2).doubleValue();
        }
        return Objects.equals(state1, state2);
    }

    /**
     * Internal class for tracking state changes and their metadata.
     * This class is used to correlate state changes between OpenHAB and HomeKit,
     * preventing feedback loops and ensuring proper event handling.
     *
     * Key implementation details:
     * - Tracks state changes with timestamps
     * - Maintains metadata for event correlation
     * - Implements expiration for cleanup
     * - Provides thread-safe access to state and metadata
     *
     * @since 1.0.0
     */
    private static class ExitEvent {
        private final State state;
        private final HomekitEventMetadata metadata;
        private final long timestamp;
        private final long correlationWindowMs = 1000; // 1 second window

        /**
         * Creates a new exit event with the given state and metadata.
         *
         * @param state The {@link State} associated with this event
         * @param metadata The {@link HomekitEventMetadata} for event correlation
         * @since 1.0.0
         */
        public ExitEvent(State state, HomekitEventMetadata metadata) {
            this.state = state;
            this.metadata = metadata;
            this.timestamp = System.currentTimeMillis();
        }

        /**
         * Checks if this event has expired based on the correlation window.
         *
         * @return true if the event has expired, false otherwise
         * @since 1.0.0
         */
        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > correlationWindowMs;
        }

        /**
         * Gets the state associated with this event.
         *
         * @return The {@link State} associated with this event
         * @since 1.0.0
         */
        public State getState() {
            return state;
        }

        /**
         * Gets the metadata associated with this event.
         *
         * @return The {@link HomekitEventMetadata} for event correlation
         * @since 1.0.0
         */
        public HomekitEventMetadata getMetadata() {
            return metadata;
        }

        /**
         * Gets the timestamp when this event was created.
         *
         * @return The timestamp in milliseconds
         * @since 1.0.0
         */
        public long getTimestamp() {
            return timestamp;
        }
    }

    /**
     * Internal class for collecting and analyzing statistics about exit events.
     * This class tracks event timing and provides statistical analysis to monitor
     * the performance and behavior of the HomeKit integration.
     *
     * Key implementation details:
     * - Thread-safe event time collection
     * - Periodic statistics reporting
     * - Histogram generation for time distribution
     * - Basic statistical calculations (mean, std dev)
     * - Automatic cleanup of old data
     *
     * @since 1.0.0
     */
    private class ExitEventStatisticsCollector {
        private final List<Long> eventTimes = new ArrayList<>();
        private final Object lock = new Object();
        private Optional<ScheduledFuture<?>> scheduledTask = Optional.empty();
        private Optional<ScheduledExecutorService> executor = Optional.empty();

        public void start() {
            synchronized (lock) {
                if (executor.isEmpty()) {
                    executor = Optional.of(ThreadPoolManager.getScheduledPool("homekit"));
                }
                if (scheduledTask.isEmpty()) {
                    scheduledTask = Optional.of(executor.get().scheduleAtFixedRate(this::printStatistics,
                            STATISTICS_REPORT_INTERVAL_SECONDS, STATISTICS_REPORT_INTERVAL_SECONDS, TimeUnit.SECONDS));
                }
            }
        }

        public void stop() {
            synchronized (lock) {
                scheduledTask.ifPresent(task -> {
                    task.cancel(false);
                    scheduledTask = Optional.empty();
                });
                executor.ifPresent(exec -> {
                    exec.shutdown();
                    executor = Optional.empty();
                });
            }
        }

        public void recordEvent(long timeMs) {
            synchronized (lock) {
                eventTimes.add(timeMs);
                if (eventTimes.size() > MAX_STATISTICS_ENTRIES) {
                    eventTimes.remove(0);
                }
            }
        }

        private void printStatistics() {
            synchronized (lock) {
                if (eventTimes.isEmpty()) {
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
                    Long value = sortedTimes.get(index);
                    deciles[i] = value.intValue();
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

                // We know these values cannot be null since they come from a non-empty ArrayList
                // that we just created and sorted.
                long minTime = sortedTimes.get(0);
                long maxTime = sortedTimes.get(sortedTimes.size() - 1);

                logger.info(
                        "Exit Event Statistics (based on {} events):\nMean: {:.2f} ms\nStd Dev: {:.2f} ms\nMin: {} ms\nMax: {} ms\n{}",
                        eventTimes.size(), mean, stdDev, minTime, maxTime, histogram);
            }
        }
    }

    /**
     * Gets all items managed by this bridge.
     *
     * @return Collection of {@link Item} instances
     * @since 1.0.0
     */
    @SuppressWarnings("null") // Stream.filter(Objects::nonNull) guarantees non-null items in result
    public Collection<Item> getItems() {
        return accessoryMap.keySet().stream().map(itemName -> Optional.ofNullable(itemRegistry.get(itemName)))
                .filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
    }

    /**
     * Gets the accessory mapped to an item.
     *
     * @param itemName The name of the item
     * @return Optional containing the mapped {@link HomekitAccessory}, or empty if
     *         not found
     * @since 1.0.0
     */
    public Optional<HomekitAccessory> getMappedAccessory(String itemName) {
        return Optional.ofNullable(accessoryMap.get(itemName));
    }
}
