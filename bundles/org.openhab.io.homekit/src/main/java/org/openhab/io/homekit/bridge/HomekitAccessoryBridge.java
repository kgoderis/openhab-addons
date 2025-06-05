package org.openhab.io.homekit.bridge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.events.EventSubscriber;
import org.openhab.core.items.events.ItemCommandEvent;
import org.openhab.core.items.events.ItemEventFactory;
import org.openhab.core.items.events.ItemStateEvent;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.UID;
import org.openhab.core.thing.link.ItemChannelLinkRegistry;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.accessory.HomekitAccessoryType;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristic;
import org.openhab.io.homekit.api.event.HomekitEvent;
import org.openhab.io.homekit.api.event.HomekitEventSubscriber;
import org.openhab.io.homekit.api.event.HomekitEventType;
import org.openhab.io.homekit.api.factory.HomekitAccessoryFactory;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.factory.HomekitServiceFactory;
import org.openhab.io.homekit.api.server.HomekitAccessoryServer;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.config.HomekitConfigurationManager;
import org.openhab.io.homekit.core.characteristic.AbstractHomekitCharacteristic;
import org.openhab.io.homekit.core.event.HomekitPeerGroupUIDImpl;
import org.openhab.io.homekit.event.core.HomekitEventMetadata;
import org.openhab.io.homekit.event.core.HomekitEventSubscription;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.event.model.characteristic.HomekitCharacteristicChangedEvent;
import org.openhab.io.homekit.event.model.characteristic.HomekitCharacteristicUpdateEvent;
import org.openhab.io.homekit.exception.HomekitFactoryException;
import org.openhab.io.homekit.server.registry.HomekitAccessoryServerRegistryImpl;
import org.openhab.io.homekit.util.HomekitUID;
import org.openhab.io.homekit.util.ItemUID;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link HomekitAccessoryBridge} manages the integration between OpenHAB
 * and HomeKit using an accessory-based
 * configuration.
 * It handles the complete lifecycle of HomeKit accessories, including creation,
 * updates, and removal.
 *
 * <p>
 * This class implements {@link EventSubscriber} to handle:
 * <ul>
 * <li>Item state changes for HomeKit-enabled items through
 * {@link ItemStateEvent}</li>
 * <li>{@link HomekitCharacteristic} value updates and synchronization</li>
 * <li>{@link HomekitAccessory} registration, updates, and cleanup</li>
 * </ul>
 * </p>
 *
 * <p>
 * The bridge maintains thread-safe collections for:
 * <ul>
 * <li>Accessories ({@link #accessoryMap}) - Maps accessory UIDs to
 * {@link HomekitAccessory} instances</li>
 * <li>Characteristics ({@link #characteristicMap}) - Maps characteristic UIDs
 * to {@link HomekitCharacteristic}
 * instances</li>
 * <li>Item/Channel mappings ({@link #itemMap}, {@link #channelMap}) - Maps
 * items and channels to their corresponding
 * characteristics</li>
 * </ul>
 * </p>
 *
 * <p>
 * The class integrates with:
 * <ul>
 * <li>{@link HomekitAccessory} for accessory lifecycle management and state
 * synchronization</li>
 * <li>{@link HomekitCharacteristic} for value conversion and validation between
 * OpenHAB and HomeKit</li>
 * <li>{@link org.openhab.core.items.Item OpenHAB's item system} for state
 * synchronization and command handling</li>
 * <li>{@link org.openhab.core.thing.Channel OpenHAB's channel system} for
 * channel-based characteristics and state
 * updates</li>
 * <li>{@link org.openhab.core.events.EventPublisher OpenHAB's event system} for
 * event propagation and state
 * changes</li>
 * <li>{@link org.openhab.core.thing.link.ItemChannelLinkRegistry OpenHAB's link
 * registry} for managing item-channel
 * associations</li>
 * </ul>
 * </p>
 *
 * <p>
 * <b>Key Features:</b>
 * <ul>
 * <li>Bidirectional state synchronization between OpenHAB and HomeKit</li>
 * <li>Support for both item-based and channel-based characteristics</li>
 * <li>Event correlation to prevent feedback loops</li>
 * <li>Thread-safe collections for concurrent access</li>
 * <li>Comprehensive error handling and logging</li>
 * </ul>
 * </p>
 *
 * @author Karel Goderis - Initial Contribution
 * @since 1.0.0
 */
@Component(service = { HomekitAccessoryBridge.class,
        EventSubscriber.class }, immediate = true) @NonNullByDefault public class HomekitAccessoryBridge
                implements EventSubscriber {
    private static final Logger logger = LoggerFactory.getLogger(HomekitAccessoryBridge.class);
    private static final String LOG_PREFIX = "Homekit Bridge: ";
    private static final String LOG_STATE = LOG_PREFIX + "State - ";
    private static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    private static final String LOG_WARN = LOG_PREFIX + "Warning - ";

    private final HomekitUID bridgeUID = new HomekitUID("bridge");
    private static final long EXIT_EVENT_TIMEOUT = 5000; // 5 seconds timeout for exit events

    private HomekitEventManager eventManager;
    private HomekitAccessoryServerRegistryImpl serverRegistry;
    private HomekitAccessoryFactory accessoryFactory;
    private HomekitCharacteristicFactory characteristicFactory;
    private HomekitServiceFactory serviceFactory;
    private HomekitConfigurationManager configManager;
    private ItemChannelLinkRegistry linkRegistry;
    private EventPublisher eventPublisher;
    private final Map<String, HomekitAccessory> accessoryMap = new ConcurrentHashMap<>();
    private final Map<String, HomekitCharacteristic<?>> characteristicMap = new ConcurrentHashMap<>();
    private final Map<String, ItemUID> itemMap = new ConcurrentHashMap<>();
    private final Map<String, ChannelUID> channelMap = new ConcurrentHashMap<>();
    private final Map<String, ExitEvent> exitEvents = new ConcurrentHashMap<>();
    private Set<HomekitUID> peerGroup;
    // Combined subscription tracking
    private final Set<HomekitEventSubscription> eventSubscriptions = ConcurrentHashMap.newKeySet();

    // Core OpenHAB services
    private final Map<String, ChannelUID> itemChannelMap = new ConcurrentHashMap<>();
    private final Map<ChannelUID, HomekitCharacteristic<?>> channelCharacteristicMap = new ConcurrentHashMap<>();

    // Statistics collection
    private static final boolean ENABLE_EXIT_EVENT_STATISTICS = true;
    private static final int MAX_STATISTICS_ENTRIES = 1000;
    private static final int STATISTICS_REPORT_INTERVAL_SECONDS = 60;
    private ExitEventStatisticsCollector statisticsCollector;

    @Activate public HomekitAccessoryBridge(@Reference HomekitEventManager eventManager,
            @Reference HomekitAccessoryServerRegistryImpl serverRegistry,
            @Reference HomekitAccessoryFactory accessoryFactory,
            @Reference HomekitCharacteristicFactory characteristicFactory,
            @Reference HomekitServiceFactory serviceFactory, @Reference HomekitConfigurationManager configManager,
            @Reference ItemChannelLinkRegistry linkRegistry, @Reference EventPublisher eventPublisher,
            Map<String, Object> properties) {

        this.eventManager = eventManager;
        this.serverRegistry = serverRegistry;
        this.accessoryFactory = accessoryFactory;
        this.characteristicFactory = characteristicFactory;
        this.serviceFactory = serviceFactory;
        this.configManager = configManager;
        this.linkRegistry = linkRegistry;
        this.eventPublisher = eventPublisher;

        // Initialize peer group
        this.peerGroup = Set.of(bridgeUID, new HomekitPeerGroupUIDImpl("openhab"),
                new HomekitPeerGroupUIDImpl("homekit"));

        // Initialize statistics collector
        this.statisticsCollector = new ExitEventStatisticsCollector();
        if (ENABLE_EXIT_EVENT_STATISTICS) {
            statisticsCollector.start();
        }

        loadConfiguration();
    }

    @Deactivate protected void deactivate() {
        if (ENABLE_EXIT_EVENT_STATISTICS) {
            statisticsCollector.stop();
        }
        // Unsubscribe all tracked subscriptions
        eventSubscriptions.forEach(sub -> {
            if (sub != null) {
                eventManager.unsubscribe(sub);
            }
        });
        eventSubscriptions.clear();
        cleanup();
    }

    /**
     * Cleans up all resources and removes all accessories.
     *
     * <p>
     * This method performs a complete cleanup of the bridge's resources:
     * <ul>
     * <li>Removes all accessories from their servers</li>
     * <li>Cleans up associated characteristics</li>
     * <li>Clears all internal maps and collections</li>
     * </ul>
     * </p>
     *
     * <p>
     * <b>Implementation Details:</b>
     * <ul>
     * <li>Iterates through all accessories in {@link #accessoryMap}</li>
     * <li>Removes each accessory from its server</li>
     * <li>Cleans up associated characteristics</li>
     * <li>Clears all internal collections</li>
     * </ul>
     * </p>
     */
    private void cleanup() {
        accessoryMap.values().forEach(accessory -> {
            try {
                serverRegistry.getAccessoryServer(accessory.getUID()).ifPresent(server -> {
                    try {
                        server.removeAccessory(accessory);
                    } catch (Exception e) {
                        logger.error("{}Failed to remove accessory from server {}: {}", LOG_PREFIX, accessory.getUID(),
                                e.getMessage());
                    }
                });
            } catch (Exception e) {
                logger.error("{}Failed to remove accessory {}: {}", LOG_PREFIX, accessory.getUID(), e.getMessage());
            }
        });

        accessoryMap.clear();
        characteristicMap.clear();
        itemMap.clear();
        channelMap.clear();
        exitEvents.clear();
    }

    /**
     * Loads the accessory configuration from the configuration manager.
     *
     * <p>
     * This method loads and processes the accessory configuration from the YAML
     * file:
     * <ul>
     * <li>Retrieves configuration from {@link HomekitConfigurationManager}</li>
     * <li>Processes each accessory in the configuration</li>
     * <li>Creates or updates accessories as needed</li>
     * <li>Removes accessories that are no longer in the configuration</li>
     * </ul>
     * </p>
     *
     * <p>
     * <b>Implementation Details:</b>
     * <ul>
     * <li>Uses {@link HomekitConfigurationManager#getConfiguration} to load
     * config</li>
     * <li>Processes each accessory in the configuration</li>
     * <li>Creates new accessories or updates existing ones</li>
     * <li>Removes accessories not present in the configuration</li>
     * </ul>
     * </p>
     */
    private void loadConfiguration() {
        // Load accessory configuration from YAML
        Map<UID, Map<String, Object>> allConfigs = configManager
                .getAllConfigurations(HomekitConfigurationManager.ConfigurationType.ACCESSORY);
        if (allConfigs.isEmpty()) {
            logger.warn("{}No accessory configuration found", LOG_PREFIX);
            return;
        }

        // Get current configuration UIDs as strings
        Set<String> configuredUids = allConfigs.keySet().stream().map(UID::toString).collect(Collectors.toSet());

        // Remove accessories that are no longer in configuration
        Set<String> currentUids = new HashSet<>(accessoryMap.keySet());
        currentUids.stream().filter(uid -> !configuredUids.contains(uid)).forEach(this::removeAccessory);

        // Process each accessory in the configuration
        allConfigs.forEach((uid, accessoryConfig) -> {
            String uidString = uid.toString();
            if (accessoryMap.containsKey(uidString)) {
                updateAccessory(uidString, accessoryConfig);
            } else {
                createAccessory(uidString, accessoryConfig);
            }
        });
    }

    /**
     * Removes an accessory from the bridge.
     *
     * <p>
     * This method handles the complete removal of an accessory:
     * <ul>
     * <li>Removes the accessory from its server</li>
     * <li>Cleans up associated characteristics</li>
     * <li>Removes the accessory from internal maps</li>
     * </ul>
     * </p>
     *
     * @param uid The unique identifier of the accessory to remove
     */
    private void removeAccessory(String uid) {
        @Nullable HomekitAccessory accessory = accessoryMap.get(uid);
        if (accessory != null) {
            try {
                // Remove from server
                serverRegistry.getAccessoryServer(accessory.getUID()).ifPresent(server -> {
                    try {
                        server.removeAccessory(accessory);
                    } catch (Exception e) {
                        logger.error("{}Failed to remove accessory from server {}: {}", LOG_PREFIX, accessory.getUID(),
                                e.getMessage());
                    }
                });

                // Clean up associated characteristics
                accessory.getServices().forEach(service -> {
                    service.getCharacteristics().forEach(characteristic -> {
                        String charUid = characteristic.getUID().toString();
                        removeCharacteristic(charUid);
                    });
                });

                // Remove from accessory map
                accessoryMap.remove(uid);
                logger.info("{}Removed orphaned accessory {}", LOG_PREFIX, uid);
            } catch (Exception e) {
                logger.error("{}Failed to remove orphaned accessory {}: {}", LOG_PREFIX, uid, e.getMessage());
            }
        }
    }

    /**
     * Updates an existing accessory with new configuration.
     *
     * <p>
     * This method updates an accessory's configuration:
     * <ul>
     * <li>Updates metadata and properties</li>
     * <li>Updates or adds services</li>
     * <li>Removes services no longer in configuration</li>
     * </ul>
     * </p>
     *
     * @param uid The unique identifier of the accessory to update
     * @param accessoryConfig The new configuration for the accessory
     */
    private void updateAccessory(String uid, Map<String, Object> accessoryConfig) {
        @Nullable HomekitAccessory accessory = accessoryMap.get(uid);
        if (accessory != null) {
            try {
                // Update metadata
                @SuppressWarnings("unchecked") Map<String, Object> metadata = (Map<String, Object>) accessoryConfig
                        .get("metadata");
                if (metadata != null) {
                    applyMetadata(accessory, metadata);
                }

                // Update services
                @SuppressWarnings("unchecked") Map<String, Object> services = (Map<String, Object>) accessoryConfig
                        .get("services");
                if (services != null) {
                    // Remove services that are no longer in config
                    Set<String> configuredServiceTypes = services.keySet();
                    accessory.getServices().stream()
                            .filter(service -> !configuredServiceTypes.contains(service.getType())).forEach(service -> {
                                service.getCharacteristics().forEach(characteristic -> {
                                    String charUid = characteristic.getUID().toString();
                                    removeCharacteristic(charUid);
                                    accessory.removeService(service);
                                });
                            });

                    // Update or add services
                    processServices(accessory, services);
                }

                logger.info("{}Updated accessory {}", LOG_PREFIX, uid);
            } catch (Exception e) {
                logger.error("{}Failed to update accessory {}: {}", LOG_PREFIX, uid, e.getMessage());
            }
        }
    }

    /**
     * Creates a new accessory from configuration.
     *
     * <p>
     * This method creates a new accessory:
     * <ul>
     * <li>Determines accessory type from configuration</li>
     * <li>Creates accessory using factory</li>
     * <li>Applies metadata and configuration</li>
     * <li>Registers accessory with server</li>
     * </ul>
     * </p>
     *
     * @param uid The unique identifier for the new accessory
     * @param accessoryConfig The configuration for the new accessory
     */
    private void createAccessory(String uid, Map<String, Object> accessoryConfig) {
        try {
            // Get accessory type
            @Nullable String type = (String) accessoryConfig.get("type");
            if (type == null) {
                logger.error("{}No type specified for accessory {}", LOG_PREFIX, uid);
                return;
            }

            // Check if type is supported, fall back to generic if not
            if (!accessoryFactory.supportsAccessoryType(type)) {
                logger.warn("{}Type {} not supported, falling back to generic type for accessory {}", LOG_PREFIX, type,
                        uid);
                type = "generic";
                if (!accessoryFactory.supportsAccessoryType(type)) {
                    logger.error("{}Generic type not supported by factory for accessory {}", LOG_PREFIX, uid);
                    return;
                }
            }

            // Create accessory
            HomekitAccessory accessory;
            try {
                accessory = accessoryFactory.createAccessory(type);

            } catch (HomekitFactoryException e) {
                logger.error("{}Failed to create accessory of type {} for {}: {}", LOG_PREFIX, type, uid,
                        e.getMessage());
                return;
            }

            // Apply metadata
            @SuppressWarnings("unchecked") Map<String, Object> metadata = (Map<String, Object>) accessoryConfig
                    .get("metadata");
            if (metadata != null) {
                applyMetadata(accessory, metadata);
            }

            // Process services
            @SuppressWarnings("unchecked") Map<String, Object> services = (Map<String, Object>) accessoryConfig
                    .get("services");
            if (services != null) {
                processServices(accessory, services);
            }

            // Register accessory
            Optional<HomekitAccessoryServer> serverOpt = serverRegistry.getAvailableBridgeAccessoryServer();
            serverOpt.ifPresent(server -> {
                try {
                    accessory.assignToServer(server);
                    accessoryMap.put(uid, accessory);
                    @Nullable HomekitAccessoryType annotation = accessory.getClass()
                            .getAnnotation(HomekitAccessoryType.class);
                    String accessoryType = annotation != null ? annotation.type() : "unknown";
                    logger.info("{}Successfully created accessory {} of type {}", LOG_PREFIX, uid, accessoryType);
                } catch (Exception e) {
                    logger.error("{}Failed to assign accessory {} to server: {}", LOG_PREFIX, uid, e.getMessage());
                }
            });
            if (serverOpt.isEmpty()) {
                logger.error("{}No available server for accessory {}", LOG_PREFIX, uid);
            }
        } catch (Exception e) {
            logger.error("{}Failed to create accessory {}: {}", LOG_PREFIX, uid, e.getMessage());
        }
    }

    /**
     * Applies metadata to an accessory.
     *
     * <p>
     * This method applies metadata to an accessory's information service:
     * <ul>
     * <li>Finds the accessory information service</li>
     * <li>Updates characteristics with metadata values</li>
     * <li>Publishes update events for changed values</li>
     * </ul>
     * </p>
     *
     * @param accessory The accessory to apply metadata to
     * @param metadata The metadata to apply
     */
    private void applyMetadata(HomekitAccessory accessory, Map<String, Object> metadata) {
        Optional<HomekitService> infoService = accessory.getService("accessoryInformation");
        infoService.ifPresent(service -> {
            metadata.forEach((key, value) -> {
                service.getCharacteristics().stream().filter(c -> c.getTag().equals(key)).findFirst().ifPresent(c -> {
                    try {
                        @SuppressWarnings("unchecked") HomekitCharacteristic<Object> characteristic = (HomekitCharacteristic<Object>) c;
                        characteristic.setValue(value);
                        HomekitCharacteristicUpdateEvent updateEvent = new HomekitCharacteristicUpdateEvent(
                                (UID) bridgeUID, (UID) c.getUID(), c,
                                characteristic.toValueJson(characteristic.getValue()),
                                characteristic.toValueJson(value), Map.<String, Object>of(),
                                new HomekitEventMetadata(bridgeUID, null, bridgeUID, peerGroup));
                        eventManager.publishEvent(updateEvent);
                    } catch (Exception e) {
                        logger.warn("{}Failed to set metadata {}: {}", LOG_PREFIX, key, e.getMessage());
                    }
                });
            });
        });
    }

    /**
     * Processes services for an accessory.
     *
     * <p>
     * This method processes the services configuration:
     * <ul>
     * <li>Creates or updates services</li>
     * <li>Processes characteristics for each service</li>
     * <li>Handles service removal</li>
     * </ul>
     * </p>
     *
     * @param accessory The accessory to process services for
     * @param services The services configuration
     */
    private void processServices(HomekitAccessory accessory, Map<String, Object> services) {
        services.forEach((serviceType, serviceConfig) -> {
            try {
                // Create or get service
                Optional<HomekitService> serviceOpt = accessory.getService(serviceType);
                @Nullable HomekitService service = null;

                if (serviceOpt.isPresent()) {
                    service = serviceOpt.get();
                } else {
                    try {
                        HomekitService newService = serviceFactory.createService(serviceType, accessory);
                        accessory.addService(newService);
                        service = newService;
                    } catch (HomekitFactoryException e) {
                        logger.error("{}Failed to create service {}: {}", LOG_PREFIX, serviceType, e.getMessage());
                    }
                }

                // Process characteristics only if service is not null
                if (service != null) {
                    @SuppressWarnings("unchecked") Map<String, Object> characteristics = (Map<String, Object>) ((Map<String, Object>) serviceConfig)
                            .get("characteristics");
                    if (characteristics != null) {
                        processCharacteristics(service, characteristics);
                    }
                }
            } catch (Exception e) {
                logger.error("{}Failed to process service {}: {}", LOG_PREFIX, serviceType, e.getMessage());
            }
        });
    }

    /**
     * Processes characteristics for a service.
     *
     * <p>
     * This method processes the characteristics configuration:
     * <ul>
     * <li>Creates characteristics using factory</li>
     * <li>Applies characteristic configuration</li>
     * <li>Sets up event subscriptions</li>
     * </ul>
     * </p>
     *
     * @param service The service to process characteristics for
     * @param characteristics The characteristics configuration
     */
    private void processCharacteristics(HomekitService service, Map<String, Object> characteristics) {
        characteristics.forEach((type, config) -> {
            try {
                HomekitCharacteristic<?> characteristic = characteristicFactory.createCharacteristic(type, service);
                if (characteristic != null) {
                    // Apply characteristic configuration
                    @SuppressWarnings("unchecked") Map<String, Object> charConfig = (Map<String, Object>) config;
                    applyCharacteristicConfig(characteristic, charConfig);

                    // Add to service
                    service.addCharacteristic(characteristic);
                    characteristicMap.put(characteristic.getUID().toString(), characteristic);

                    // Set up event subscription and track it
                    HomekitEventSubscription subscription = subscribeToCharacteristicEvents(characteristic);
                    if (subscription != null) {
                        eventSubscriptions.add(subscription);
                    }
                }
            } catch (Exception e) {
                logger.error("{}Failed to create characteristic {}: {}", LOG_PREFIX, type, e.getMessage());
            }
        });
    }

    /**
     * Applies configuration to a characteristic.
     *
     * <p>
     * This method applies configuration to a characteristic:
     * <ul>
     * <li>Handles item/channel mapping</li>
     * <li>Applies optional setters (min/max/step/inverted)</li>
     * <li>Updates internal maps</li>
     * </ul>
     * </p>
     *
     * @param characteristic The characteristic to configure
     * @param config The configuration to apply
     */
    private void applyCharacteristicConfig(HomekitCharacteristic<?> characteristic, Map<String, Object> config) {
        // Handle item/channel mapping
        String itemName = (String) config.get("item");
        String channelId = (String) config.get("channel");
        if (itemName != null) {
            try {
                ItemUID itemUID = new ItemUID(itemName);
                itemMap.put(characteristic.getUID().toString(), itemUID);
            } catch (IllegalArgumentException e) {
                logger.warn("{}Invalid item UID format {}: {}", LOG_PREFIX, itemName, e.getMessage());
            }
        }
        if (channelId != null) {
            channelMap.put(characteristic.getUID().toString(), new ChannelUID(channelId));
        }

        // Apply other configuration (optional setters)
        if (characteristic instanceof AbstractHomekitCharacteristic<?> abstractCharacteristic) {
            if (config.containsKey("minValue")) {
                @Nullable Object value = config.get("minValue");
                try {
                    var m = abstractCharacteristic.getClass().getMethod("withMinValue", double.class);
                    if (value instanceof Number) {
                        m.invoke(abstractCharacteristic, ((Number) value).doubleValue());
                    }
                } catch (Exception ignored) {
                }
            }
            if (config.containsKey("maxValue")) {
                @Nullable Object value = config.get("maxValue");
                try {
                    var m = abstractCharacteristic.getClass().getMethod("withMaxValue", double.class);
                    if (value instanceof Number) {
                        m.invoke(abstractCharacteristic, ((Number) value).doubleValue());
                    }
                } catch (Exception ignored) {
                }
            }
            if (config.containsKey("step")) {
                @Nullable Object value = config.get("step");
                try {
                    var m = abstractCharacteristic.getClass().getMethod("withStep", double.class);
                    if (value instanceof Number) {
                        m.invoke(abstractCharacteristic, ((Number) value).doubleValue());
                    }
                } catch (Exception ignored) {
                }
            }
            if (config.containsKey("inverted")) {
                @Nullable Object value = config.get("inverted");
                try {
                    var m = abstractCharacteristic.getClass().getMethod("withInverted", Boolean.class);
                    if (value instanceof Boolean) {
                        m.invoke(abstractCharacteristic, value);
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }

    /**
     * Subscribes to characteristic events.
     *
     * <p>
     * This method sets up event subscription for a characteristic:
     * <ul>
     * <li>Creates event subscription</li>
     * <li>Sets up event handler</li>
     * <li>Tracks subscription for cleanup</li>
     * </ul>
     * </p>
     *
     * @param characteristic The characteristic to subscribe to
     * @return The created event subscription
     */
    private HomekitEventSubscription subscribeToCharacteristicEvents(HomekitCharacteristic<?> characteristic) {
        return eventManager.subscribe(HomekitEventType.CHARACTERISTIC_VALUE_CHANGED, (UID) characteristic.getUID(),
                bridgeUID, new HomekitEventSubscriber() {
                    @Override public void onEvent(HomekitEvent event) {
                        handleCharacteristicEvent(characteristic, event);
                    }

                    @Override public void onEventError(HomekitEvent event, Exception e) {
                        logger.error("{}Failed to handle characteristic event: {}", LOG_PREFIX, e.getMessage());
                    }
                });
    }

    /**
     * Handles characteristic events.
     *
     * <p>
     * This method processes characteristic events:
     * <ul>
     * <li>Checks event origin to prevent loops</li>
     * <li>Handles channel-based characteristics</li>
     * <li>Handles item-based characteristics</li>
     * <li>Updates item states</li>
     * </ul>
     * </p>
     *
     * @param characteristic The characteristic that triggered the event
     * @param event The event to handle
     */
    private void handleCharacteristicEvent(HomekitCharacteristic<?> characteristic, HomekitEvent event) {
        if (event instanceof HomekitCharacteristicChangedEvent changedEvent) {
            try {
                // Check if event is from peer group
                if (event.getMetadata().isFromPeerGroup(peerGroup)) {
                    logger.debug("{}Ignoring event from peer group: {}", LOG_PREFIX,
                            event.getMetadata().getImmediateOrigin());
                    return;
                }

                // First check if this is a channel-based characteristic
                @Nullable ChannelUID channelUID = channelMap.get(characteristic.getUID().toString());
                if (channelUID != null) {
                    // This is a channel-based characteristic
                    changedEvent.getNewValue().ifPresent(newValue -> {
                        State newState = characteristic.toState(newValue);
                        if (newState != null) {
                            // Find linked items for this channel
                            linkRegistry.getLinks(channelUID).forEach(link -> {
                                String itemName = link.getItemName();
                                exitEvents.put(itemName, new ExitEvent(newState, event.getMetadata()));
                                eventPublisher.post(ItemEventFactory.createStateEvent(itemName, newState));
                            });
                        }
                    });
                } else {
                    // This is an item-based characteristic
                    @Nullable ItemUID itemUID = itemMap.get(characteristic.getUID().toString());
                    if (itemUID != null) {
                        changedEvent.getNewValue().ifPresent(newValue -> {
                            State newState = characteristic.toState(newValue);
                            if (newState != null) {
                                exitEvents.put(itemUID.toString(), new ExitEvent(newState, event.getMetadata()));
                                eventPublisher.post(ItemEventFactory.createStateEvent(itemUID.getItemName(), newState));
                            }
                        });
                    }
                }
            } catch (Exception e) {
                logger.error("{}Failed to handle characteristic event: {}", LOG_PREFIX, e.getMessage());
            }
        }
    }

    @Override public Set<String> getSubscribedEventTypes() {
        return Set.of(ItemCommandEvent.TYPE, ItemStateEvent.TYPE);
    }

    @Override public void receive(Event event) {
        if (event instanceof ItemCommandEvent commandEvent) {
            handleItemCommand(commandEvent);
        } else if (event instanceof ItemStateEvent stateEvent) {
            handleItemState(stateEvent);
        }
    }

    /**
     * Handles an item event by converting it to a HomeKit characteristic update.
     *
     * <p>
     * This method processes item events (commands or states) and converts them to
     * HomeKit characteristic
     * updates. It handles both channel-based and item-based characteristics,
     * manages event correlation
     * to prevent loops, and publishes the appropriate events.
     * </p>
     *
     * <p>
     * <b>Key implementation details:</b>
     * <ul>
     * <li>Converts item values to HomeKit characteristic values using
     * {@link HomekitCharacteristic#toValueJson}</li>
     * <li>Manages exit event correlation to prevent feedback loops through
     * {@link ExitEvent}</li>
     * <li>Creates appropriate event metadata using
     * {@link HomekitEventMetadata}</li>
     * <li>Publishes characteristic update events using
     * {@link HomekitCharacteristicUpdateEvent}</li>
     * </ul>
     * </p>
     *
     * <p>
     * <b>Event Flow:</b>
     * <ol>
     * <li>Convert input value to HomeKit characteristic value</li>
     * <li>Check for existing exit event correlation</li>
     * <li>Create appropriate event metadata</li>
     * <li>Publish characteristic update event</li>
     * <li>Clean up exit event if used</li>
     * </ol>
     * </p>
     *
     * @param itemName The name of the item being processed
     * @param value The command or state value to be processed
     * @param characteristic The HomeKit characteristic to update
     */
    private void handleItemEvent(String itemName, Object value, @Nullable HomekitCharacteristic<?> characteristic) {
        if (characteristic == null) {
            logger.warn("{}Cannot handle event for item {}: characteristic is null", LOG_WARN, itemName);
            return;
        }
        try {
            logger.debug("{}Starting to process event for item {} with value {} and characteristic {}", LOG_STATE,
                    itemName, value, characteristic.getUID());

            @SuppressWarnings("unchecked") HomekitCharacteristic<Object> typedCharacteristic = (HomekitCharacteristic<Object>) characteristic;
            JsonValue jsonValue = typedCharacteristic.toValueJson(value);

            if (jsonValue != null) {
                // Check if state has actually changed
                @Nullable ExitEvent exitEvent = exitEvents.get(itemName);
                if (exitEvent != null && !exitEvent.isExpired() && statesEqual(value, exitEvent.getState())) {
                    logger.debug("{}State unchanged for item {}, skipping event processing", LOG_STATE, itemName);
                    return;
                }

                logger.debug("{}Successfully converted value {} to HomeKit format for item {}", LOG_STATE, value,
                        itemName);

                // Check for exit event correlation
                if (exitEvent != null && !exitEvent.isExpired()) {
                    logger.debug("{}Found valid exit event correlation for item {}, using existing metadata", LOG_STATE,
                            itemName);

                    // Use the metadata from the exit event to prevent loops
                    HomekitCharacteristicUpdateEvent updateEvent = new HomekitCharacteristicUpdateEvent(characteristic,
                            null, // old value
                            jsonValue, Collections.emptyMap(), exitEvent.getMetadata());
                    eventManager.publishEvent(updateEvent);
                    exitEvents.remove(itemName);
                    statisticsCollector.recordEvent(System.currentTimeMillis() - exitEvent.getTimestamp());
                    logger.debug("{}Published correlated update event for item {}", LOG_STATE, itemName);
                } else {
                    logger.debug("{}No valid exit event correlation found for item {}, creating new metadata",
                            LOG_STATE, itemName);

                    // Create new metadata for uncorrelated event
                    HomekitEventMetadata metadata = new HomekitEventMetadata((UID) bridgeUID, null, (UID) bridgeUID,
                            peerGroup);
                    HomekitCharacteristicUpdateEvent updateEvent = new HomekitCharacteristicUpdateEvent(characteristic,
                            null, // old value
                            jsonValue, Collections.emptyMap(), metadata);
                    eventManager.publishEvent(updateEvent);
                    logger.debug("{}Published uncorrelated update event for item {}", LOG_STATE, itemName);
                }
            } else {
                logger.warn("{}Failed to convert value {} to HomeKit format for item {}", LOG_WARN, value, itemName);
            }
        } catch (Exception e) {
            logger.error("{}Failed to handle event for item {} with value {}: {}", LOG_ERROR, itemName, value,
                    e.getMessage(), e);
        }
    }

    /**
     * Handles item command events from OpenHAB.
     *
     * <p>
     * This method processes item command events and converts them to HomeKit
     * characteristic
     * updates. It supports both channel-based and item-based characteristics.
     * </p>
     *
     * <p>
     * <b>Key implementation details:</b>
     * <ul>
     * <li>Validates input parameters and event structure</li>
     * <li>Cleans up expired exit events to prevent memory leaks</li>
     * <li>Handles both channel and item-based characteristics</li>
     * <li>Delegates to {@link #handleItemEvent} for actual processing</li>
     * </ul>
     * </p>
     *
     * <p>
     * <b>Event Flow:</b>
     * <ol>
     * <li>Validate input event and command</li>
     * <li>Clean up expired exit events</li>
     * <li>Determine characteristic type (channel or item-based)</li>
     * <li>Process command through appropriate characteristic</li>
     * </ol>
     * </p>
     *
     * @param event The {@link ItemCommandEvent} to process
     */
    public void handleItemCommand(ItemCommandEvent event) {
        if (event == null || event.getItemName() == null) {
            logger.warn("{}Received null or invalid item command event", LOG_WARN);
            return;
        }

        logger.debug("{}Processing item command event: {}", LOG_STATE, event);

        // Clean up expired exit events
        cleanupExpiredExitEvents();

        String itemName = event.getItemName();
        Command command = event.getItemCommand();
        if (command == null) {
            logger.warn("{}Received null command for item {}", LOG_WARN, itemName);
            return;
        }

        logger.debug("{}Processing command {} for item {}", LOG_STATE, command, itemName);

        // First check if this item is linked to a channel
        @Nullable ChannelUID channelUID = itemChannelMap.get(itemName);
        if (channelUID != null) {
            logger.debug("{}Item {} is linked to channel {}", LOG_STATE, itemName, channelUID);

            // This is a channel-based characteristic
            @Nullable HomekitCharacteristic<?> characteristic = channelCharacteristicMap.get(channelUID);
            if (characteristic != null) {
                logger.debug("{}Found channel-based characteristic {} for item {}", LOG_STATE, characteristic.getUID(),
                        itemName);
                handleItemEvent(itemName, command, characteristic);
            } else {
                logger.warn("{}No characteristic found for channel {} of item {}", LOG_WARN, channelUID, itemName);
            }
        } else {
            logger.debug("{}Item {} is not linked to any channel, checking for direct characteristic", LOG_STATE,
                    itemName);

            // This is an item-based characteristic
            @Nullable HomekitCharacteristic<?> characteristic = characteristicMap.get(itemName);
            if (characteristic != null) {
                logger.debug("{}Found item-based characteristic {} for item {}", LOG_STATE, characteristic.getUID(),
                        itemName);
                handleItemEvent(itemName, command, characteristic);
            } else {
                logger.warn("{}No characteristic found for item {}", LOG_WARN, itemName);
            }
        }
    }

    /**
     * Handles item state events from OpenHAB.
     *
     * <p>
     * This method processes item state events and converts them to HomeKit
     * characteristic
     * updates. It supports both channel-based and item-based characteristics.
     * </p>
     *
     * <p>
     * <b>Key implementation details:</b>
     * <ul>
     * <li>Validates input parameters and event structure</li>
     * <li>Cleans up expired exit events to prevent memory leaks</li>
     * <li>Handles both channel and item-based characteristics</li>
     * <li>Delegates to {@link #handleItemEvent} for actual processing</li>
     * </ul>
     * </p>
     *
     * <p>
     * <b>Event Flow:</b>
     * <ol>
     * <li>Validate input event and state</li>
     * <li>Clean up expired exit events</li>
     * <li>Determine characteristic type (channel or item-based)</li>
     * <li>Process state through appropriate characteristic</li>
     * </ol>
     * </p>
     *
     * @param event The {@link ItemStateEvent} to process
     */
    public void handleItemState(ItemStateEvent event) {
        if (event == null || event.getItemName() == null) {
            logger.warn("{}Received null or invalid item state event", LOG_WARN);
            return;
        }

        logger.debug("{}Processing item state event: {}", LOG_STATE, event);

        // Clean up expired exit events
        cleanupExpiredExitEvents();

        String itemName = event.getItemName();
        State state = event.getItemState();
        if (state == null) {
            logger.warn("{}Received null state for item {}", LOG_WARN, itemName);
            return;
        }

        logger.debug("{}Processing state {} for item {}", LOG_STATE, state, itemName);

        // First check if this item is linked to a channel
        @Nullable ChannelUID channelUID = itemChannelMap.get(itemName);
        if (channelUID != null) {
            logger.debug("{}Item {} is linked to channel {}", LOG_STATE, itemName, channelUID);

            // This is a channel-based characteristic
            @Nullable HomekitCharacteristic<?> characteristic = channelCharacteristicMap.get(channelUID);
            if (characteristic != null) {
                logger.debug("{}Found channel-based characteristic {} for item {}", LOG_STATE, characteristic.getUID(),
                        itemName);
                handleItemEvent(itemName, state, characteristic);
            } else {
                logger.warn("{}No characteristic found for channel {} of item {}", LOG_WARN, channelUID, itemName);
            }
        } else {
            logger.debug("{}Item {} is not linked to any channel, checking for direct characteristic", LOG_STATE,
                    itemName);

            // This is an item-based characteristic
            @Nullable HomekitCharacteristic<?> characteristic = characteristicMap.get(itemName);
            if (characteristic != null) {
                logger.debug("{}Found item-based characteristic {} for item {}", LOG_STATE, characteristic.getUID(),
                        itemName);
                handleItemEvent(itemName, state, characteristic);
            } else {
                logger.warn("{}No characteristic found for item {}", LOG_WARN, itemName);
            }
        }
    }

    /**
     * Cleans up expired exit events.
     *
     * <p>
     * This method removes expired exit events from the map to prevent memory leaks.
     * Exit events are used to track event propagation and prevent feedback loops.
     * </p>
     */
    private void cleanupExpiredExitEvents() {
        exitEvents.entrySet().removeIf(entry -> {
            @Nullable ExitEvent event = entry.getValue();
            return event != null && event.isExpired();
        });
    }

    /**
     * Compares two states for equality.
     *
     * <p>
     * This method provides a null-safe comparison of two states:
     * <ul>
     * <li>Handles null values</li>
     * <li>Uses equals for non-null values</li>
     * <li>Optimizes for reference equality</li>
     * </ul>
     * </p>
     *
     * @param state1 The first state to compare
     * @param state2 The second state to compare
     * @return true if the states are equal, false otherwise
     */
    private boolean statesEqual(Object state1, Object state2) {
        if (state1 == state2)
            return true;
        if (state1 == null || state2 == null)
            return false;
        return state1.equals(state2);
    }

    /**
     * Represents an exit event used for event correlation.
     *
     * <p>
     * This class tracks events that have been processed to prevent feedback loops:
     * <ul>
     * <li>Stores the state that triggered the event</li>
     * <li>Stores event metadata for correlation</li>
     * <li>Tracks event expiration</li>
     * </ul>
     * </p>
     */
    private static class ExitEvent {
        private final State state;
        private final HomekitEventMetadata metadata;
        private final long timestamp;

        /**
         * Creates a new exit event.
         *
         * @param state The state that triggered the event
         * @param metadata The event metadata for correlation
         */
        public ExitEvent(State state, HomekitEventMetadata metadata) {
            this.state = state;
            this.metadata = metadata;
            this.timestamp = System.currentTimeMillis();
        }

        /**
         * Checks if this exit event has expired.
         *
         * @return true if the event has expired, false otherwise
         */
        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > EXIT_EVENT_TIMEOUT;
        }

        /**
         * Gets the state that triggered this event.
         *
         * @return The state that triggered the event
         */
        public State getState() {
            return state;
        }

        /**
         * Gets the metadata for this event.
         *
         * @return The event metadata
         */
        public HomekitEventMetadata getMetadata() {
            return metadata;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }

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
                    logger.info("{}No exit event statistics available yet", LOG_PREFIX);
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
                    @Nullable Long value = sortedTimes.get(index);
                    deciles[i] = value != null ? value.intValue() : 0;
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
                        "{}Exit Event Statistics (based on {} events):\n" + "Mean: {:.2f} ms\n" + "Std Dev: {:.2f} ms\n"
                                + "Min: {} ms\n" + "Max: {} ms\n" + "{}",
                        LOG_PREFIX, eventTimes.size(), mean, stdDev, sortedTimes.get(0),
                        sortedTimes.get(sortedTimes.size() - 1), histogram);
            }
        }
    }

    // When removing a characteristic, also unsubscribe its event subscription
    private void removeCharacteristic(String charUid) {
        characteristicMap.remove(charUid);
        itemMap.remove(charUid);
        channelMap.remove(charUid);
    }
}
