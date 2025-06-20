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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.SafeCaller;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.events.EventSubscriber;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemFactory;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.ItemStateConverter;
import org.openhab.core.items.ItemUtil;
import org.openhab.core.items.events.ItemCommandEvent;
import org.openhab.core.items.events.ItemEventFactory;
import org.openhab.core.items.events.ItemStateEvent;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingRegistryChangeListener;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.UID;
import org.openhab.core.thing.link.ItemChannelLink;
import org.openhab.core.thing.link.ItemChannelLinkRegistry;
import org.openhab.core.thing.profiles.Profile;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.ProfileContext;
import org.openhab.core.thing.profiles.ProfileFactory;
import org.openhab.core.thing.profiles.ProfileTypeUID;
import org.openhab.core.thing.profiles.StateProfile;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.TimeSeries;
import org.openhab.core.types.Type;
import org.openhab.io.homekit.HomekitBindingConstants;
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
import org.openhab.io.homekit.core.characteristic.AbstractHomekitCharacteristic;
import org.openhab.io.homekit.core.event.HomekitPeerGroupUIDImpl;
import org.openhab.io.homekit.event.core.HomekitEventMetadata;
import org.openhab.io.homekit.event.core.HomekitEventSubscription;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.event.manager.HomekitEventManager.HomekitEventHandler;
import org.openhab.io.homekit.event.model.characteristic.HomekitCharacteristicChangedEvent;
import org.openhab.io.homekit.event.model.characteristic.HomekitCharacteristicUpdateEvent;
import org.openhab.io.homekit.exception.HomekitAccessoryOperationException;
import org.openhab.io.homekit.exception.HomekitFactoryException;
import org.openhab.io.homekit.util.HomekitUID;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the integration between OpenHAB Things and HomeKit accessories.
 *
 * This class implements both {@link org.openhab.core.events.EventSubscriber}
 * and
 * {@link org.openhab.core.thing.ThingRegistryChangeListener}
 * to handle bidirectional communication between OpenHAB and HomeKit. It manages
 * the lifecycle of HomeKit accessories,
 * their services, and characteristics, while ensuring proper state
 * synchronization and event handling.
 *
 * Key implementation details:
 * - Manages bidirectional state/command conversion using
 * {@link org.openhab.core.thing.profiles.Profile profiles}
 * - Handles thing lifecycle events (addition, removal, updates)
 * - Manages orphaned accessories to prevent controller deletion
 * - Implements event correlation to prevent feedback loops
 * - Provides statistics collection for performance monitoring
 *
 * The class integrates with:
 * - {@link org.openhab.core.thing.ThingRegistry} for thing management
 * - {@link org.openhab.core.thing.link.ItemChannelLinkRegistry} for link
 * management
 * - {@link org.openhab.core.thing.profiles.ProfileFactory} for profile creation
 * - {@link org.openhab.io.homekit.api.registry.HomekitAccessoryServerRegistry}
 * for server management
 * - {@link org.openhab.io.homekit.config.HomekitConfigurationManager} for
 * configuration management
 * - {@link org.openhab.core.events.EventPublisher} for event handling
 * - {@link org.openhab.io.homekit.api.factory.HomekitAccessoryFactory} for
 * accessory creation
 * - {@link org.openhab.io.homekit.api.factory.HomekitServiceFactory} for
 * service creation
 * - {@link org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory} for
 * characteristic creation
 * - {@link org.openhab.io.homekit.event.manager.HomekitEventManager} for event
 * management
 * - {@link org.openhab.core.common.SafeCaller} for safe method execution
 * - {@link org.openhab.core.items.ItemStateConverter} for state conversion
 * - {@link org.openhab.core.items.ItemRegistry} for item management
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0.0
 */
@Component(service = { EventSubscriber.class, ThingRegistryChangeListener.class, HomekitThingBridge.class })
@NonNullByDefault
public class HomekitThingBridge implements EventSubscriber, ThingRegistryChangeListener {
    private static final String LOG_PREFIX = "HomekitThingBridge: ";
    private static final Logger logger = LoggerFactory.getLogger(HomekitThingBridge.class);

    // Configuration key for orphan functionality
    private static final String CONFIG_ORPHAN_ENABLED = "orphanEnabled";
    private boolean orphanEnabled = true; // Default to true for backward compatibility

    // Event tracking
    private static final boolean ENABLE_EXIT_EVENT_STATISTICS = true;
    private static final int MAX_STATISTICS_ENTRIES = 1000;
    private static final int STATISTICS_REPORT_INTERVAL_SECONDS = 60;
    private final Map<String, ExitEvent> exitEvents = new ConcurrentHashMap<>();
    private final ExitEventStatisticsCollector statisticsCollector;
    private final Set<HomekitUID> peerGroup;

    // Core OpenHAB services
    private final ThingRegistry thingRegistry;
    private final ItemChannelLinkRegistry itemChannelLinkRegistry;
    private final ProfileFactory profileFactory;
    private final HomekitAccessoryServerRegistry accessoryServerRegistry;
    private final HomekitConfigurationManager configManager;
    private final EventPublisher eventPublisher;
    private final HomekitAccessoryFactory accessoryFactory;
    private final HomekitCharacteristicFactory characteristicFactory;
    private final HomekitServiceFactory serviceFactory;
    private final ItemChannelLinkRegistry linkRegistry;
    private final ItemRegistry itemRegistry;
    private final SafeCaller safeCaller;
    private final ItemStateConverter itemStateConverter;
    // Maps to store profiles for each direction of communication
    private final Map<ChannelUID, Profile> channelProfiles = new ConcurrentHashMap<>();
    private final Map<ChannelUID, Profile> channelHomekitToOpenhabProfiles = new ConcurrentHashMap<>();

    // Maps to store relationships between Things, Channels, and HomeKit
    // accessories/characteristics
    private final Map<ThingUID, HomekitAccessory> thingAccessoryMap = new ConcurrentHashMap<>();
    private final Map<ChannelUID, HomekitCharacteristic<?>> channelCharacteristicMap = new ConcurrentHashMap<>();
    private final Map<HomekitCharacteristic<?>, ChannelUID> characteristicChannelMap = new ConcurrentHashMap<>();
    private final Map<String, ChannelUID> itemChannelMap = new ConcurrentHashMap<>();
    private final HomekitEventManager eventManager;
    private final Set<HomekitEventSubscription> eventSubscriptions = new HashSet<>();
    private final HomekitUID bridgeUID = new HomekitUID("bridge");
    private final Set<ItemFactory> itemFactories = new CopyOnWriteArraySet<>();
    private final Map<String, List<Class<? extends Command>>> acceptedCommandTypeMap = new ConcurrentHashMap<>();

    /**
     * Creates a new {@link HomekitThingBridge} instance.
     *
     * This method initializes the bridge with all required services and
     * configurations.
     * It sets up event listeners, loads existing things, and configures statistics
     * collection.
     *
     * Key implementation details:
     * - Initializes service dependencies
     * - Loads orphan configuration
     * - Sets up thing registry listener
     * - Initializes existing HomeKit tagged things
     * - Configures event statistics collection
     *
     * @param thingRegistry The {@link ThingRegistry} service
     * @param itemChannelLinkRegistry The {@link ItemChannelLinkRegistry} service
     * @param profileFactory The {@link ProfileFactory} service
     * @param accessoryServerRegistry The {@link HomekitAccessoryServerRegistry}
     *            service
     * @param configManager The {@link HomekitConfigurationManager}
     *            service
     * @param eventPublisher The {@link EventPublisher} service
     * @param accessoryFactory The {@link HomekitAccessoryFactory} service
     * @param characteristicFactory The {@link HomekitCharacteristicFactory}
     *            service
     * @param serviceFactory The {@link HomekitServiceFactory} service
     * @param linkRegistry The {@link ItemChannelLinkRegistry} service
     * @param eventManager The {@link HomekitEventManager} service
     * @param safeCaller The {@link SafeCaller} service
     * @param itemStateConverter The {@link ItemStateConverter} service
     * @param itemRegistry The {@link ItemRegistry} service
     * @param properties The component properties
     * @since 1.0.0
     */
    @Activate
    public HomekitThingBridge(@Reference ThingRegistry thingRegistry,
            @Reference ItemChannelLinkRegistry itemChannelLinkRegistry, @Reference ProfileFactory profileFactory,
            @Reference HomekitAccessoryServerRegistry accessoryServerRegistry,
            @Reference HomekitConfigurationManager configManager, @Reference EventPublisher eventPublisher,
            @Reference HomekitAccessoryFactory accessoryFactory,
            @Reference HomekitCharacteristicFactory characteristicFactory,
            @Reference HomekitServiceFactory serviceFactory, @Reference ItemChannelLinkRegistry linkRegistry,
            @Reference HomekitEventManager eventManager, final @Reference SafeCaller safeCaller,
            final @Reference ItemStateConverter itemStateConverter, final @Reference ItemRegistry itemRegistry,
            Map<String, Object> properties) {
        this.thingRegistry = thingRegistry;
        this.itemChannelLinkRegistry = itemChannelLinkRegistry;
        this.profileFactory = profileFactory;
        this.accessoryServerRegistry = accessoryServerRegistry;
        this.configManager = configManager;
        this.eventPublisher = eventPublisher;
        this.accessoryFactory = accessoryFactory;
        this.characteristicFactory = characteristicFactory;
        this.serviceFactory = serviceFactory;
        this.linkRegistry = linkRegistry;
        this.eventManager = eventManager;
        this.safeCaller = safeCaller;
        this.itemStateConverter = itemStateConverter;
        this.itemRegistry = itemRegistry;

        // Load orphan configuration
        Object orphanConfig = properties.getOrDefault(CONFIG_ORPHAN_ENABLED, "true");
        String configValue = orphanConfig.toString();
        this.orphanEnabled = Boolean.parseBoolean(configValue);
        logger.info("{}Orphan functionality is {}", LOG_PREFIX, orphanEnabled ? "enabled" : "disabled");

        // Initialize existing Things
        initializeExistingThings();

        this.peerGroup = Set.of(bridgeUID, new HomekitPeerGroupUIDImpl("openhab"),
                new HomekitPeerGroupUIDImpl("homekit"));
        this.statisticsCollector = new ExitEventStatisticsCollector();

        if (ENABLE_EXIT_EVENT_STATISTICS) {
            statisticsCollector.start();
        }
    }

    /**
     * Initializes existing things in the registry.
     *
     * This method processes all things currently in the registry to set up
     * HomeKit accessories and characteristics.
     *
     * @since 1.0.0
     */
    private void initializeExistingThings() {
        thingRegistry.getAll().forEach(this::processThing);
    }

    /**
     * Processes a thing to set up its HomeKit integration.
     *
     * This method handles the configuration and setup of HomeKit accessories for a
     * given thing.
     * It performs the following steps:
     * 1. Retrieves and validates the thing's configuration
     * 2. Creates a HomeKit accessory using the appropriate factory
     * 3. Sets up bidirectional event forwarding
     * 4. Configures characteristics and services
     *
     * Key implementation details:
     * - Validates bridge configuration
     * - Creates accessory using {@link HomekitAccessoryFactory}
     * - Sets up event subscriptions
     * - Configures metadata and characteristics
     *
     * @param thing The {@link org.openhab.core.thing.Thing} to process
     * @throws IllegalStateException if no available bridge accessory server is
     *             found
     * @since 1.0.0
     */
    private void processThing(Thing thing) {
        // Get configuration for the Thing from the thingToAccessory mapping
        Optional<Map<String, Object>> thingConfigOpt = configManager.getConfiguration(thing.getUID(),
                HomekitConfigurationManager.ConfigurationType.THING);

        if (thingConfigOpt.isEmpty()) {
            logger.debug("{}Thing {} not configured for bridging", LOG_PREFIX, thing.getUID());
            return;
        }

        @SuppressWarnings("null") // Safe after isEmpty() check above
        Map<String, Object> thingConfig = thingConfigOpt.get();
        if (!Boolean.TRUE.equals(thingConfig.get("bridge"))) {
            logger.debug("{}Thing {} not configured as bridge", LOG_PREFIX, thing.getUID());
            return;
        }

        // Create HomeKit accessory for the Thing
        Optional<HomekitAccessoryServer> serverOpt = accessoryServerRegistry.getAvailableBridgeAccessoryServer();
        if (serverOpt.isEmpty()) {
            logger.warn("{}No available bridge accessory server found for thing {}", LOG_PREFIX, thing.getUID());
            return;
        }

        try {
            HomekitAccessory accessory = createAccessoryForThing(thing, serverOpt.get(), thingConfig);
            thingAccessoryMap.put(thing.getUID(), accessory);
            processChannels(thing, accessory);
        } catch (Exception e) {
            logger.error("{}Failed to create accessory for thing {}: {}", LOG_PREFIX, thing.getUID(), e.getMessage(),
                    e);
        }
    }

    /**
     * Creates a HomeKit accessory for a thing.
     *
     * This method creates and configures a HomeKit accessory based on the thing's
     * configuration.
     * It handles the following aspects:
     * 1. Determines the accessory type from configuration
     * 2. Creates the accessory using the factory
     * 3. Applies metadata and configuration
     * 4. Assigns the accessory to a server
     *
     * Key implementation details:
     * - Uses {@link HomekitAccessoryFactory} for accessory creation
     * - Applies metadata from configuration
     * - Handles unit conversion and state mapping
     * - Manages server assignment
     *
     * @param thing The {@link org.openhab.core.thing.Thing} to create an
     *            accessory for
     * @param server The
     *            {@link org.openhab.io.homekit.api.server.HomekitAccessoryServer}
     *            to register with
     * @param thingConfig The configuration for the thing
     * @return The created
     *         {@link org.openhab.io.homekit.api.accessory.HomekitAccessory}, or
     *         null if creation fails
     * @throws HomekitAccessoryOperationException if accessory creation or server
     *             assignment fails
     * @throws HomekitFactoryException if accessory creation fails
     * @since 1.0.0
     */
    private HomekitAccessory createAccessoryForThing(Thing thing, HomekitAccessoryServer server,
            Map<String, Object> thingConfig) throws HomekitFactoryException, HomekitAccessoryOperationException {
        // Get accessory type from configuration
        String accessoryType = (String) thingConfig.get("accessory");
        if (accessoryType == null) {
            accessoryType = "generic"; // Default to generic if not specified
        }

        // Create accessory based on Thing type
        HomekitAccessory accessory;
        try {
            accessory = accessoryFactory.createAccessory(accessoryType);
        } catch (HomekitFactoryException e) {
            logger.error("{}Failed to create accessory of type {}: {}", LOG_PREFIX, accessoryType, e.getMessage());
            throw e;
        }

        // Apply metadata from configuration
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) thingConfig.get("metadata");
        if (metadata != null) {
            // Create an accessory information service to store metadata
            Optional<HomekitService> infoService = accessory.getService("accessoryInformation");
            if (infoService.isPresent()) {
                HomekitService service = infoService.get();
                metadata.forEach((key, value) -> {
                    try {
                        // Find the characteristic by tag and update its value
                        service.getCharacteristics().stream().filter(c -> c.getTag().equals(key)).findFirst()
                                .ifPresent(c -> {
                                    try {
                                        if (value instanceof String) {
                                            c.setValue(Json.createArrayBuilder().add((String) value).build().get(0));
                                        } else if (value instanceof Number) {
                                            c.setValue(Json.createArrayBuilder().add(((Number) value).doubleValue())
                                                    .build().get(0));
                                        } else if (value instanceof Boolean) {
                                            c.setValue((Boolean) value ? JsonValue.TRUE : JsonValue.FALSE);
                                        }
                                    } catch (Exception e) {
                                        logger.warn("{}Failed to set metadata {} on accessory: {}", LOG_PREFIX, key,
                                                e.getMessage());
                                    }
                                });
                    } catch (Exception e) {
                        logger.warn("{}Failed to set metadata {} on accessory: {}", LOG_PREFIX, key, e.getMessage());
                    }
                });
            }
        }

        try {
            accessory.assignToServer(server);
        } catch (HomekitAccessoryOperationException e) {
            logger.error("{}Failed to assign accessory to server: {}", LOG_PREFIX, e.getMessage(), e);
            throw e;
        }
        return accessory;
    }

    /**
     * Processes channels for a thing.
     *
     * This method sets up HomeKit services and characteristics for each channel in
     * the thing.
     * It performs the following steps:
     * 1. Retrieves service mappings from configuration
     * 2. Creates or retrieves services for each channel
     * 3. Configures characteristics for each service
     * 4. Sets up event subscriptions and profiles
     *
     * Key implementation details:
     * - Uses {@link HomekitServiceFactory} for service creation
     * - Configures characteristics using {@link HomekitCharacteristicFactory}
     * - Sets up bidirectional event forwarding
     * - Creates profiles for state/command conversion
     *
     * @param thing The {@link org.openhab.core.thing.Thing} whose channels to
     *            process
     * @param accessory The
     *            {@link org.openhab.io.homekit.api.accessory.HomekitAccessory}
     *            to add services to
     * @throws IllegalStateException if service or characteristic creation fails
     * @since 1.0.0
     */
    private void processChannels(Thing thing, HomekitAccessory accessory) {
        // Get service mappings for this thing
        Optional<Map<String, Object>> serviceMappingsOpt = configManager.getConfiguration(thing.getUID(),
                HomekitConfigurationManager.ConfigurationType.CHANNEL);

        @Nullable
        Map<String, Object> serviceMappings = serviceMappingsOpt.orElse(Collections.emptyMap());
        if (serviceMappings == null) {
            serviceMappings = Collections.emptyMap(); // Additional safety check
        }

        for (Channel channel : thing.getChannels()) {
            String channelUID = channel.getUID().toString();

            // Try to find a matching service mapping
            @SuppressWarnings("null") // serviceMappings is guaranteed non-null from orElse() above
            Optional<Map<String, Object>> serviceConfig = findMatchingServiceConfig(channelUID, serviceMappings);
            if (serviceConfig.isEmpty()) {
                continue;
            }

            @SuppressWarnings("null") // serviceConfig.isEmpty() check ensures get() is safe
            Map<String, Object> config = serviceConfig.get();
            String serviceTag = (String) config.get("serviceTag");
            if (serviceTag == null) {
                continue;
            }

            try {
                // Create or get the service
                Optional<HomekitService> serviceOpt = accessory.getService(serviceTag);
                HomekitService service;
                if (serviceOpt.isEmpty()) {
                    try {
                        service = serviceFactory.createService(serviceTag, accessory);
                        accessory.addService(service);
                    } catch (HomekitFactoryException e) {
                        logger.error("{}Failed to create service {}: {}, falling back to generic service", LOG_PREFIX,
                                serviceTag, e.getMessage());
                        try {
                            // Fallback to generic service
                            service = serviceFactory.createService("generic", accessory);
                            accessory.addService(service);
                        } catch (HomekitFactoryException fallbackException) {
                            logger.error("{}Failed to create fallback generic service: {}", LOG_PREFIX,
                                    fallbackException.getMessage());
                            continue; // Only skip if even generic service creation fails
                        }
                    }
                } else {
                    @SuppressWarnings("null") // Safe after isEmpty() check above
                    HomekitService nonNullService = serviceOpt.get();
                    service = nonNullService;
                }

                // Process characteristics for this service
                @SuppressWarnings({ "unchecked", "null" }) // Cast to expected type and config.get() checked for null
                Map<String, Object> characteristics = (Map<String, Object>) config.get("characteristics");
                if (characteristics != null) {
                    processCharacteristics(channel, service, characteristics);
                }
            } catch (Exception e) {
                logger.error("{}Failed to process channel {}: {}", LOG_PREFIX, channelUID, e.getMessage(), e);
            }
        }
    }

    /**
     * Finds matching service configuration for a channel.
     *
     * @param channelUID The {@link ChannelUID} to find configuration for
     * @param serviceMappings The service mappings to search in
     * @return Optional containing the matching service configuration
     * @since 1.0.0
     */
    private Optional<Map<String, Object>> findMatchingServiceConfig(String channelUID,
            Map<String, Object> serviceMappings) {
        // First try exact match
        if (serviceMappings.containsKey(channelUID)) {
            @SuppressWarnings({ "unchecked", "null" }) // Map.get() for known key and cast to expected type
            Map<String, Object> config = (Map<String, Object>) serviceMappings.get(channelUID);
            return Optional.ofNullable(config);
        }

        // Then try pattern matching
        for (Map.Entry<String, Object> entry : serviceMappings.entrySet()) {
            String pattern = entry.getKey();
            if (pattern.contains("*")) {
                String regex = pattern.replace("*", ".*");
                if (channelUID.matches(regex)) {
                    @SuppressWarnings({ "unchecked", "null" }) // Cast to expected type is safe here
                    Map<String, Object> config = (Map<String, Object>) entry.getValue();
                    return Optional.ofNullable(config);
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Processes characteristics for a channel.
     *
     * This method creates and configures HomeKit characteristics for a channel.
     *
     * @param channel The {@link Channel} to process
     * @param service The {@link HomekitService} to add characteristics to
     * @param characteristics The characteristic configuration
     * @since 1.0.0
     */
    private void processCharacteristics(Channel channel, HomekitService service, Map<String, Object> characteristics) {
        for (Map.Entry<String, Object> entry : characteristics.entrySet()) {
            String characteristicType = entry.getKey();
            Object value = entry.getValue();
            if (!(value instanceof Map)) {
                continue; // Skip invalid entries
            }
            @SuppressWarnings({ "unchecked", "null" }) // Cast to expected type is safe here
            Map<String, Object> config = (Map<String, Object>) value;

            try {
                HomekitCharacteristic<?> characteristic = characteristicFactory.createCharacteristic(characteristicType,
                        service);
                // Apply characteristic configuration
                if (config.containsKey("inverted")
                        && characteristic instanceof AbstractHomekitCharacteristic<?> abstractCharacteristic) {
                    // Set the inverted property through the characteristic's configuration
                    Object invertedValue = config.get("inverted");
                    if (invertedValue instanceof Boolean invertedBoolean) {
                        JsonObjectBuilder builder = Json.createObjectBuilder();
                        builder.add("inverted", invertedBoolean);
                        abstractCharacteristic.setValue(builder.build());
                    }
                }

                // Add bidirectional mapping
                channelCharacteristicMap.put(channel.getUID(), characteristic);
                characteristicChannelMap.put(characteristic, channel.getUID());
                service.addCharacteristic(characteristic);

                // Subscribe to characteristic events
                subscribeToCharacteristicEvents(characteristic);

                // Find linked Item and create profiles
                linkRegistry.getLinks(channel.getUID()).forEach(link -> {
                    itemChannelMap.put(link.getItemName(), channel.getUID());
                    createProfileForChannel(channel, link);
                });
            } catch (Exception e) {
                logger.error("{}Failed to create characteristic {} for channel {}: {}", LOG_PREFIX, characteristicType,
                        channel.getUID(), e.getMessage(), e);
            }
        }
    }

    /**
     * Subscribes to characteristic events.
     *
     * This method sets up event subscriptions for characteristic value changes.
     *
     * @param characteristic The {@link HomekitCharacteristic} to subscribe to
     * @since 1.0.0
     */
    private void subscribeToCharacteristicEvents(HomekitCharacteristic<?> characteristic) {
        try {

            // Subscribe to value changes
            eventSubscriptions.add(eventManager.subscribe(HomekitEventType.CHARACTERISTIC_VALUE_CHANGED,
                    (UID) characteristic.getUID(), (UID) bridgeUID,
                    (HomekitEventHandler) (event -> handleCharacteristicEvent(characteristic, event))));

            // Subscribe to state changes
            eventSubscriptions.add(eventManager.subscribe(HomekitEventType.CHARACTERISTIC_STATE_CHANGED,
                    (UID) characteristic.getUID(), (UID) bridgeUID,
                    (HomekitEventHandler) (event -> handleCharacteristicEvent(characteristic, event))));

            logger.debug("{}Subscribed to events for characteristic {}", LOG_PREFIX, characteristic.getUID());
        } catch (Exception e) {
            logger.error("{}Failed to subscribe to characteristic events: {}", LOG_PREFIX, e.getMessage(), e);
        }
    }

    /**
     * Unsubscribes from characteristic events.
     *
     * This method removes event subscriptions for characteristic value changes.
     *
     * @param characteristic The {@link HomekitCharacteristic} to unsubscribe from
     * @since 1.0.0
     */
    private void unsubscribeFromCharacteristicEvents(HomekitCharacteristic<?> characteristic) {
        try {
            // Find and remove all subscriptions for this characteristic
            eventSubscriptions.removeIf(subscription -> {
                if (subscription.getPublisherUID().equals((UID) characteristic.getUID())) {
                    eventManager.unsubscribe(subscription.getEventType(), subscription.getPublisherUID(),
                            subscription.getSubscriber());
                    return true;
                }
                return false;
            });
            logger.debug("{}Unsubscribed from events for characteristic {}", LOG_PREFIX, characteristic.getUID());
        } catch (Exception e) {
            logger.error("{}Failed to unsubscribe from characteristic events: {}", LOG_PREFIX, e.getMessage(), e);
        }
    }

    /**
     * Handles characteristic events.
     *
     * This method processes events when characteristic values change.
     * It performs the following steps:
     * 1. Validates the event type and characteristic
     * 2. Finds the associated channel and item
     * 3. Checks for peer group events to prevent feedback loops
     * 4. Updates the item state using profiles or direct conversion
     *
     * Key implementation details:
     * - Uses {@link org.openhab.core.thing.profiles.Profile} for state conversion
     * - Implements peer group event filtering
     * - Manages state conversion and validation
     * - Tracks exit events for correlation
     *
     * @param characteristic The
     *            {@link org.openhab.io.homekit.api.characteristic.HomekitCharacteristic}
     *            that changed
     * @param event The
     *            {@link org.openhab.io.homekit.api.event.HomekitEvent}
     *            to handle
     * @throws IllegalStateException if state conversion or update fails
     * @since 1.0.0
     */
    private void handleCharacteristicEvent(HomekitCharacteristic<?> characteristic, HomekitEvent event) {
        if (event instanceof HomekitCharacteristicChangedEvent changedEvent) {
            try {
                // Find the channel for this characteristic using the bidirectional map
                @Nullable
                ChannelUID channelUID = characteristicChannelMap.get(characteristic);
                if (channelUID != null) {
                    // Check if event is from peer group
                    if (event.getMetadata().isFromPeerGroup(peerGroup)) {
                        logger.debug("{}Ignoring Homekit event from peer group: {}", LOG_PREFIX,
                                event.getMetadata().getImmediateOrigin());
                        return;
                    }

                    // Use the HomeKit -> OpenHAB profile
                    @Nullable
                    Profile profile = channelHomekitToOpenhabProfiles.get(channelUID);
                    if (profile instanceof StateProfile stateProfile) {
                        changedEvent.getNewValue().ifPresent(newValue -> {
                            State newState = characteristic.toState(newValue);
                            // Store the exit event
                            itemChannelLinkRegistry.getLinks(channelUID).stream().findFirst()
                                    .map(ItemChannelLink::getItemName).ifPresent(itemName -> {
                                        exitEvents.put(itemName, new ExitEvent(newState, event.getMetadata()));
                                    });
                            stateProfile.onStateUpdateFromHandler(newState);
                        });
                    } else {
                        // If no profile is used, directly handle the state
                        changedEvent.getNewValue().ifPresent(newValue -> {
                            State newState = characteristic.toState(newValue);
                            itemChannelLinkRegistry.getLinks(channelUID).stream().findFirst()
                                    .map(ItemChannelLink::getItemName).ifPresent(itemName -> {
                                        exitEvents.put(itemName, new ExitEvent(newState, event.getMetadata()));
                                        eventPublisher.post(ItemEventFactory.createStateEvent(itemName, newState));
                                    });
                        });
                    }
                }
            } catch (Exception e) {
                logger.error("{}Failed to handle characteristic event: {}", LOG_PREFIX, e.getMessage(), e);
            }
        }
    }

    /**
     * Gets the subscribed event types.
     *
     * @return Set of subscribed event type strings
     * @since 1.0.0
     */
    @Override
    public Set<String> getSubscribedEventTypes() {
        return Set.of(ItemCommandEvent.TYPE, ItemStateEvent.TYPE);
    }

    /**
     * Receives and processes events.
     *
     * This method handles incoming events from the event bus.
     *
     * @param event The {@link Event} to process
     * @since 1.0.0
     */
    @Override
    public void receive(Event event) {
        if (event instanceof ItemCommandEvent commandEvent) {
            handleItemCommand(commandEvent);
        } else if (event instanceof ItemStateEvent stateEvent) {
            handleItemState(stateEvent);
        }
    }

    /**
     * Handles item command events.
     *
     * This method processes command events from items and forwards them to HomeKit.
     * It performs the following steps:
     * 1. Validates the command and finds the associated channel
     * 2. Locates the corresponding HomeKit characteristic
     * 3. Uses profiles for command conversion if available
     * 4. Publishes the command as a HomeKit event
     *
     * Key implementation details:
     * - Uses {@link org.openhab.core.thing.profiles.Profile} for command conversion
     * - Handles command type conversion and validation
     * - Manages event correlation to prevent feedback loops
     * - Ensures thread-safe event publishing
     *
     * @param event The {@link org.openhab.core.items.events.ItemCommandEvent} to
     *            handle
     * @throws IllegalStateException if characteristic lookup fails
     * @since 1.0.0
     */
    private void handleItemCommand(ItemCommandEvent event) {
        String itemName = event.getItemName();
        Command command = event.getItemCommand();

        // Find the Channel linked to this Item
        @Nullable
        ChannelUID channelUID = itemChannelMap.get(itemName);
        if (channelUID == null) {
            return;
        }

        // Find the Characteristic for this Channel
        @Nullable
        HomekitCharacteristic<?> characteristic = channelCharacteristicMap.get(channelUID);
        if (characteristic == null) {
            return;
        }

        // Check if we have a profile for this channel
        @Nullable
        Profile profile = channelProfiles.get(channelUID);
        if (profile != null) {
            // Use the profile to handle the command
            if (profile instanceof StateProfile stateProfile) {
                stateProfile.onCommandFromItem(command);
            }
        } else {
            // Direct communication - convert and send event
            try {
                HomekitCharacteristic<?> nonNullCharacteristic = Objects.requireNonNull(characteristic);
                HomekitCharacteristicUpdateEvent updateEvent = new HomekitCharacteristicUpdateEvent((UID) bridgeUID,
                        (UID) nonNullCharacteristic.getUID(), nonNullCharacteristic,
                        nonNullCharacteristic.toValueJson((State) command), null, Map.of(),
                        new HomekitEventMetadata(bridgeUID, null, bridgeUID, Set.of()));
                eventManager.publishEvent(updateEvent);
            } catch (Exception e) {
                logger.error("{}Failed to handle command for item {}: {}", LOG_PREFIX, itemName, e.getMessage(), e);
            }
        }
    }

    /**
     * Handles item state events.
     *
     * This method processes state events from items and forwards them to HomeKit.
     * It performs the following steps:
     * 1. Validates the state and finds the associated channel
     * 2. Locates the corresponding HomeKit characteristic
     * 3. Checks for correlated events to prevent feedback loops
     * 4. Uses profiles for state conversion if available
     * 5. Publishes the state as a HomeKit event
     *
     * Key implementation details:
     * - Uses {@link org.openhab.core.thing.profiles.Profile} for state conversion
     * - Implements event correlation using {@link ExitEvent}
     * - Handles state type conversion and validation
     * - Manages statistics collection for performance monitoring
     *
     * @param event The {@link org.openhab.core.items.events.ItemStateEvent} to
     *            handle
     * @throws IllegalStateException if characteristic lookup fails
     * @since 1.0.0
     */
    private void handleItemState(ItemStateEvent event) {
        String itemName = event.getItemName();
        State state = event.getItemState();

        // Check if this is a correlated event (originated from HomeKit)
        String eventSource = event.getSource();
        @SuppressWarnings("null") // bridgeUID is final non-null field
        String bridgeUIDString = bridgeUID.toString();
        if (eventSource != null && eventSource.equals(bridgeUIDString)) {
            logger.debug("{}Dropping correlated event for item {}", LOG_PREFIX, itemName);
            return;
        }

        // Find the Channel linked to this Item (itemChannelMap is not null)
        @Nullable
        ChannelUID channelUID = itemChannelMap.get(itemName);
        if (channelUID == null) {
            return;
        }

        // Find the Characteristic for this Channel (channelCharacteristicMap is not null)
        @Nullable
        HomekitCharacteristic<?> characteristic = channelCharacteristicMap.get(channelUID);
        if (characteristic == null) {
            return;
        }

        // Clean up expired exit events
        exitEvents.entrySet().removeIf(entry -> entry.getValue().isExpired());

        // Check if we have a profile for this channel
        Optional.ofNullable(channelProfiles.get(channelUID)).ifPresent(profile -> {
            // Use the profile to handle the state
            if (profile instanceof StateProfile stateProfile) {
                stateProfile.onStateUpdateFromItem(state);
            }
        });

        if (Optional.ofNullable(channelProfiles.get(channelUID)).isEmpty()) {
            // Direct communication - convert and send event
            try {
                @Nullable
                ExitEvent exitEvent = exitEvents.get(itemName);
                if (exitEvent != null && !exitEvent.isExpired()) {
                    if (isStateEqual(exitEvent.getState(), state)) {
                        logger.debug("{}Processing correlated state change for item: {}", LOG_PREFIX, itemName);
                        if (ENABLE_EXIT_EVENT_STATISTICS) {
                            statisticsCollector.recordEvent(System.currentTimeMillis() - exitEvent.getTimestamp());
                        }
                        HomekitCharacteristicUpdateEvent updateEvent = new HomekitCharacteristicUpdateEvent(
                                (UID) bridgeUID, (UID) characteristic.getUID(), characteristic,
                                characteristic.toValueJson(exitEvent.getState()), characteristic.toValueJson(state),
                                Map.of(), exitEvent.getMetadata());
                        eventManager.publishEvent(updateEvent);
                        exitEvents.remove(itemName);
                    }
                }

                // Handle uncorrelated state change
                if (exitEvent == null) {
                    logger.debug("{}Processing new state change for item: {}", LOG_PREFIX, itemName);
                    HomekitCharacteristic<?> nonNullCharacteristic = Objects.requireNonNull(characteristic);
                    HomekitCharacteristicUpdateEvent updateEvent = new HomekitCharacteristicUpdateEvent((UID) bridgeUID,
                            (UID) nonNullCharacteristic.getUID(), nonNullCharacteristic,
                            nonNullCharacteristic.toValueJson(state), null, Map.of(),
                            new HomekitEventMetadata(bridgeUID, null, bridgeUID, peerGroup));
                    eventManager.publishEvent(updateEvent);
                }
            } catch (Exception e) {
                logger.error("{}Failed to handle state update for item {}: {}", LOG_PREFIX, itemName, e.getMessage(),
                        e);
            }
        }
    }

    /**
     * Checks if two states are equal.
     *
     * This method performs a deep comparison of two states.
     * It handles the following cases:
     * 1. Direct reference equality
     * 2. Null state handling
     * 3. State type-specific equality
     *
     * Key implementation details:
     * - Handles null states safely
     * - Uses state-specific equality checks
     * - Supports all OpenHAB state types
     *
     * @param state1 The first {@link org.openhab.core.types.State}
     * @param state2 The second {@link org.openhab.core.types.State}
     * @return true if the states are equal, false otherwise
     * @since 1.0.0
     */
    private boolean isStateEqual(State state1, State state2) {
        if (Objects.equals(state1, state2)) {
            return true;
        }
        // States are @NonNull by annotation in method signature
        // The null check is redundant since we're using @NonNullByDefault at the class level
        if (state1 instanceof DecimalType && state2 instanceof DecimalType) {
            return ((DecimalType) state1).doubleValue() == ((DecimalType) state2).doubleValue();
        }
        return false;
    }

    /**
     * Creates profiles for a channel.
     *
     * This method sets up bidirectional profiles for state/command conversion
     * between OpenHAB and HomeKit.
     * It performs the following steps:
     * 1. Retrieves profile configuration from the link
     * 2. Creates OpenHAB to HomeKit profile
     * 3. Creates HomeKit to OpenHAB profile
     * 4. Sets up profile callbacks for event handling
     *
     * Key implementation details:
     * - Uses {@link org.openhab.core.thing.profiles.ProfileFactory} for profile
     * creation
     * - Configures profile callbacks for bidirectional communication
     * - Handles profile context and configuration
     * - Manages thread pool for profile execution
     *
     * @param channel The {@link org.openhab.core.thing.Channel} to create profiles
     *            for
     * @param link The {@link org.openhab.core.thing.link.ItemChannelLink}
     *            associated with the channel
     * @throws IllegalStateException if profile creation fails
     * @since 1.0.0
     */
    private void createProfileForChannel(Channel channel, ItemChannelLink link) {
        // Get the profile type from the link's metadata
        Profile openhabToHomekitProfile;

        String profileTypeStr = (String) link.getConfiguration().get("profile");
        if (profileTypeStr == null || profileTypeStr.trim().isEmpty()) {
            logger.debug("{}No profile configured for link {}, using NoOpProfile", LOG_PREFIX, link.getUID());
            openhabToHomekitProfile = new NoOpProfile(new ProfileCallback() {
                @Override
                public void handleCommand(Command command) {
                    handleProfileCommand(channel.getUID(), command);
                }

                @Override
                public void sendCommand(Command command) {
                    handleProfileCommand(channel.getUID(), command);
                }

                @Override
                public ItemChannelLink getItemChannelLink() {
                    return link;
                }

                @Override
                public void sendTimeSeries(TimeSeries timeSeries) {
                    // No-op: HomeKit doesn't use time series
                }

                @Override
                public void sendUpdate(State state) {
                    handleProfileState(channel.getUID(), state);
                }
            });
        } else {

            // Create the profile context using OpenHAB's thread pool
            ProfileContext context = new ProfileContext() {

                @Override
                public Configuration getConfiguration() {
                    return link.getConfiguration();
                }

                @Override
                public ScheduledExecutorService getExecutorService() {
                    return ThreadPoolManager.getScheduledPool(HomekitBindingConstants.THREAD_POOL_NAME);
                }
            };

            ProfileTypeUID profileTypeUID = new ProfileTypeUID("system", profileTypeStr);

            // Create OpenHAB -> HomeKit profile
            openhabToHomekitProfile = profileFactory.createProfile(profileTypeUID, new ProfileCallback() {
                @Override
                public void handleCommand(Command command) {
                    handleProfileCommand(channel.getUID(), command);
                }

                @Override
                public void sendCommand(Command command) {
                    handleProfileCommand(channel.getUID(), command);
                }

                @Override
                public ItemChannelLink getItemChannelLink() {
                    return link;
                }

                @Override
                public void sendTimeSeries(TimeSeries timeSeries) {
                    // No-op: HomeKit doesn't use time series
                }

                @Override
                public void sendUpdate(State state) {
                    handleProfileState(channel.getUID(), state);
                }
            }, context);

            // Create HomeKit -> OpenHAB profile
            Profile homekitToOpenhabProfile = profileFactory.createProfile(profileTypeUID,
                    new HomekitProfileCallbackImpl(eventPublisher, itemStateConverter, link, thingRegistry::get,
                            itemName -> getItem(itemName)
                                    .orElseThrow(() -> new IllegalArgumentException("Item not found: " + itemName)),
                            (cmd, ch, item) -> {
                                if (ch != null && item != null && cmd != null) {
                                    Command result = toAcceptedCommand(cmd, ch, item).orElse(null);
                                    return result != null ? result : cmd;
                                }
                                return cmd;
                            }),
                    context);

            if (homekitToOpenhabProfile != null) {
                channelHomekitToOpenhabProfiles.put(channel.getUID(), homekitToOpenhabProfile);
            }
        }

        // Store both profiles
        if (openhabToHomekitProfile != null) {
            channelProfiles.put(channel.getUID(), openhabToHomekitProfile);
        }
    }

    /**
     * Gets an item by name.
     *
     * @param itemName The name of the item
     * @return Optional containing the {@link Item}, or empty if not found
     * @since 1.0.0
     */
    private Optional<Item> getItem(final String itemName) {
        @Nullable
        Item item = itemRegistry.get(itemName);
        return item != null ? Optional.of(item) : Optional.empty();
    }

    /**
     * Converts to accepted command.
     *
     * This method converts a {@link Command} to a type acceptable by the target {@link Channel}.
     * 
     * @param originalType The original {@link Command} to convert
     * @param channel The {@link Channel} to convert for
     * @param item The {@link Item} to convert for
     * @return Optional containing the converted command, or empty if conversion failed
     * @since 1.0.0
     */
    public Optional<Command> toAcceptedCommand(Command originalType, @Nullable Channel channel, @Nullable Item item) {
        if (item == null || channel == null) {
            logger.warn("Trying to convert types for non-existing channel or item, discarding command.");
            return Optional.empty();
        }
        String channelAcceptedItemType = channel.getAcceptedItemType();

        if (channelAcceptedItemType == null) {
            return Optional.of(originalType);
        }

        Optional<Command> uomCommand = fixUoM(originalType, channel, item);
        if (uomCommand.isPresent()) {
            return uomCommand;
        }

        // handle HSBType/PercentType
        if (CoreItemFactory.DIMMER.equals(channelAcceptedItemType) && originalType instanceof HSBType hsb) {
            PercentType midresult = hsb.as(PercentType.class);
            return Optional.ofNullable(midresult);
        }

        // check for other cases if the type is acceptable
        List<Class<? extends Command>> acceptedTypes = acceptedCommandTypeMap.get(channelAcceptedItemType);
        if (acceptedTypes == null || acceptedTypes.contains(originalType.getClass())) {
            return Optional.of(originalType);
        } else if (acceptedTypes.contains(PercentType.class) && originalType instanceof State state
                && PercentType.class.isAssignableFrom(originalType.getClass())) {
            PercentType percentType = state.as(PercentType.class);
            return Optional.ofNullable(percentType);
        } else if (acceptedTypes.contains(OnOffType.class) && originalType instanceof State state
                && PercentType.class.isAssignableFrom(originalType.getClass())) {
            OnOffType onOffType = state.as(OnOffType.class);
            return Optional.ofNullable(onOffType);
        } else {
            logger.debug("Received not accepted type '{}' for channel '{}'", originalType.getClass().getSimpleName(),
                    channel.getUID());
            return Optional.empty();
        }
    }

    /**
     * Calculates accepted command types.
     *
     * This method determines which command types are accepted by each channel.
     *
     * @since 1.0.0
     */
    private synchronized void calculateAcceptedTypes() {
        acceptedCommandTypeMap.clear();
        for (ItemFactory itemFactory : itemFactories) {
            for (String itemTypeName : itemFactory.getSupportedItemTypes()) {
                Item item = itemFactory.createItem(itemTypeName, "tmp");
                if (item != null) {
                    acceptedCommandTypeMap.put(itemTypeName, item.getAcceptedCommandTypes());
                } else {
                    logger.error("Item factory {} suggested it can create items of type {} but returned null",
                            itemFactory, itemTypeName);
                }
            }
        }
    }

    /**
     * Fixes unit of measurement for a type.
     *
     * This method handles unit conversion and validation for types.
     * It performs the following steps:
     * 1. Validates channel and item types
     * 2. Handles backward compatibility for number channels
     * 3. Adds units to DecimalType when dimensions match
     * 4. Converts between compatible unit types
     *
     * Key implementation details:
     * - Uses {@link org.openhab.core.library.CoreItemFactory} for type validation
     * - Handles {@link org.openhab.core.library.types.QuantityType} conversion
     * - Manages unit dimension matching
     * - Supports backward compatibility
     *
     * @param originalType The original {@link org.openhab.core.types.Type}
     * @param channel The {@link org.openhab.core.thing.Channel} to fix for
     * @param item The {@link org.openhab.core.items.Item} to fix for
     * @return The fixed type, or null if fixing failed
     * @throws IllegalArgumentException if unit conversion fails
     * @since 1.0.0
     */
    @SuppressWarnings("unchecked")
    private <T extends Type> Optional<T> fixUoM(@Nullable T originalType, Channel channel, Item item) {
        String channelAcceptedItemType = channel.getAcceptedItemType();

        if (channelAcceptedItemType == null) {
            return originalType != null ? Optional.of(originalType) : Optional.empty();
        }

        // handle Number-Channels for backward compatibility
        if (CoreItemFactory.NUMBER.equals(channelAcceptedItemType)
                && originalType instanceof QuantityType<?> quantityType) {
            // strip unit from QuantityType for channels that accept plain number
            return Optional.of((T) new DecimalType(quantityType.toBigDecimal()));
        }

        String itemDimension = ItemUtil.getItemTypeExtension(item.getType());
        String channelDimension = ItemUtil.getItemTypeExtension(channelAcceptedItemType);

        if (originalType instanceof DecimalType decimalType && channelDimension != null
                && channelDimension.equals(itemDimension)) {
            // Add unit from item to DecimalType when dimensions are equal
            Unit<?> unit = Objects.requireNonNull(((NumberItem) item).getUnit());
            return Optional.of((T) new QuantityType<>(decimalType.toBigDecimal(), unit));
        }
        return Optional.empty();
    }

    /**
     * Adds an item factory.
     *
     * @param itemFactory The {@link ItemFactory} to add
     * @since 1.0.0
     */
    @Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC)
    protected void addItemFactory(ItemFactory itemFactory) {
        itemFactories.add(itemFactory);
        calculateAcceptedTypes();
    }

    /**
     * Removes an item factory.
     *
     * @param itemFactory The {@link ItemFactory} to remove
     * @since 1.0.0
     */
    protected void removeItemFactory(ItemFactory itemFactory) {
        itemFactories.remove(itemFactory);
        calculateAcceptedTypes();
    }

    /**
     * Handles profile commands.
     *
     * This method processes commands from profiles.
     *
     * @param channelUID The {@link ChannelUID} to handle commands for
     * @param command The {@link Command} to handle
     * @since 1.0.0
     */
    private void handleProfileCommand(ChannelUID channelUID, Command command) {
        HomekitCharacteristic<?> characteristic = channelCharacteristicMap.get(channelUID);
        if (characteristic == null) {
            logger.debug("{}Channel UID {} is not mapped to a characteristic", LOG_PREFIX, channelUID);
            return;
        }

        logger.debug("{}Received command {} for channel {}", LOG_PREFIX, command, channelUID);
        try {
            HomekitCharacteristicUpdateEvent updateEvent = new HomekitCharacteristicUpdateEvent((UID) bridgeUID,
                    (UID) characteristic.getUID(), characteristic, characteristic.toValueJson((State) command), null,
                    Map.of(), new HomekitEventMetadata(bridgeUID, null, bridgeUID, Set.of()));
            eventManager.publishEvent(updateEvent);
        } catch (Exception e) {
            logger.error("{}Failed to handle command for channel {}: {}", LOG_PREFIX, channelUID, e.getMessage(), e);
        }
    }

    /**
     * Handles profile states.
     *
     * This method processes states from profiles.
     *
     * @param channelUID The {@link ChannelUID} to handle states for
     * @param state The {@link State} to handle
     * @since 1.0.0
     */
    private void handleProfileState(ChannelUID channelUID, State state) {
        @Nullable
        HomekitCharacteristic<?> characteristic = channelCharacteristicMap.get(channelUID);
        if (characteristic != null) {
            try {
                HomekitCharacteristicUpdateEvent updateEvent = new HomekitCharacteristicUpdateEvent((UID) bridgeUID,
                        (UID) characteristic.getUID(), characteristic, characteristic.toValueJson(state), null,
                        Map.of(), new HomekitEventMetadata(bridgeUID, null, bridgeUID, Set.of()));
                eventManager.publishEvent(updateEvent);
            } catch (Exception e) {
                logger.error("{}Failed to handle state update for channel {}: {}", LOG_PREFIX, channelUID,
                        e.getMessage(), e);
            }
        }
    }

    /**
     * Deactivates the HomeKit thing bridge.
     *
     * <p>
     * This method performs cleanup operations when the component is deactivated:
     * </p>
     * <ul>
     * <li>Stops statistics collection</li>
     * <li>Unsubscribes from all event subscriptions</li>
     * <li>Cleans up channel profiles and mappings</li>
     * <li>Removes all accessories from servers</li>
     * <li>Ensures proper resource cleanup</li>
     * </ul>
     *
     * <p>
     * <b>Key implementation details:</b>
     * </p>
     * <ul>
     * <li>Stops statistics collector to prevent memory leaks</li>
     * <li>Unsubscribes from all event subscriptions</li>
     * <li>Cleans up all channel mappings and profiles</li>
     * <li>Removes accessories from servers to prevent orphaned references</li>
     * <li>Logs deactivation for debugging</li>
     * </ul>
     */
    @Deactivate
    protected void deactivate() {
        logger.info("{}Deactivating HomeKit thing bridge", LOG_PREFIX);

        // Stop statistics collection
        if (ENABLE_EXIT_EVENT_STATISTICS) {
            statisticsCollector.stop();
            logger.debug("{}Statistics collection stopped", LOG_PREFIX);
        }

        // Unsubscribe from all event subscriptions
        eventSubscriptions.forEach(eventManager::unsubscribe);
        eventSubscriptions.clear();
        logger.debug("{}Unsubscribed from {} event subscriptions", LOG_PREFIX, eventSubscriptions.size());

        // Clean up channel profiles
        channelProfiles.clear();
        channelHomekitToOpenhabProfiles.clear();
        logger.debug("{}Cleaned up channel profiles", LOG_PREFIX);

        // Clean up mappings
        channelCharacteristicMap.clear();
        characteristicChannelMap.clear();
        itemChannelMap.clear();
        logger.debug("{}Cleaned up channel mappings", LOG_PREFIX);

        // Remove all accessories from servers
        thingAccessoryMap.forEach((thingUID, accessory) -> {
            try {
                // Find the server that contains this accessory and remove it
                accessoryServerRegistry.getAll().stream().filter(server -> {
                    try {
                        return server.getAccessories().contains(accessory);
                    } catch (HomekitAccessoryOperationException e) {
                        logger.warn("{}Failed to get accessories from server {}: {}", LOG_PREFIX, server.getUID(),
                                e.getMessage());
                        return false;
                    }
                }).findFirst().ifPresent(server -> {
                    try {
                        server.removeAccessory(accessory);
                        logger.debug("{}Removed accessory {} from server {}", LOG_PREFIX, accessory.getUID(),
                                server.getUID());
                    } catch (HomekitAccessoryOperationException e) {
                        logger.warn("{}Failed to remove accessory {} from server {}: {}", LOG_PREFIX,
                                accessory.getUID(), server.getUID(), e.getMessage());
                    }
                });
            } catch (Exception e) {
                logger.warn("{}Failed to process accessory {} during deactivation: {}", LOG_PREFIX, accessory.getUID(),
                        e.getMessage());
            }
        });

        // Clear accessory map
        thingAccessoryMap.clear();
        logger.debug("{}Cleaned up accessory mappings", LOG_PREFIX);

        logger.info("{}HomeKit thing bridge deactivated successfully", LOG_PREFIX);
    }

    /**
     * A no-operation profile that does not perform any state/command conversion.
     * Used as a fallback when no specific profile is configured.
     */
    private static class NoOpProfile implements StateProfile {
        private final ProfileCallback callback;

        public NoOpProfile(ProfileCallback callback) {
            this.callback = callback;
        }

        @Override
        public ProfileTypeUID getProfileTypeUID() {
            return new ProfileTypeUID("homekit", "noop");
        }

        @Override
        public void onStateUpdateFromItem(State state) {
            // Forward state to handler
            callback.sendUpdate(state);
        }

        @Override
        public void onStateUpdateFromHandler(State state) {
            // Forward state to item
            callback.sendUpdate(state);
        }

        @Override
        public void onCommandFromItem(Command command) {
            // Forward command to handler
            callback.handleCommand(command);
        }

        @Override
        public void onCommandFromHandler(Command command) {
            // Forward command to item
            callback.handleCommand(command);
        }
    }

    /**
     * Handles thing addition events.
     *
     * This method processes events when things are added to the registry.
     * It performs the following steps:
     * 1. Checks if the thing is a restoration of an orphaned accessory
     * 2. Attempts to restore orphaned accessories if enabled
     * 3. Processes new things to set up HomeKit integration
     * 4. Configures accessories and characteristics
     *
     * Key implementation details:
     * - Uses {@link #isOrphaned(Thing)} to check orphan status
     * - Implements orphan restoration logic
     * - Manages accessory lifecycle
     * - Ensures thread-safe operations
     *
     * @param thing The {@link org.openhab.core.thing.Thing} that was added
     * @throws IllegalStateException if accessory creation or restoration fails
     * @since 1.0.0
     */
    @Override
    public void added(Thing thing) {
        logger.debug("{}Thing {} added", LOG_PREFIX, thing.getUID());

        if (orphanEnabled) {
            // Check if this is a restoration of an orphaned accessory
            @Nullable
            HomekitAccessory existingAccessory = thingAccessoryMap.get(thing.getUID());
            if (existingAccessory != null && existingAccessory.isOrphaned()) {
                if (restoreOrphanedAccessory(thing, existingAccessory)) {
                    logger.info("{}Successfully restored orphaned accessory for thing {}", LOG_PREFIX, thing.getUID());
                    return;
                }
            }
        }

        // Process the new thing if it's not a restoration or if orphan functionality is
        // disabled
        processThing(thing);
    }

    /**
     * Handles thing removal events.
     *
     * This method processes events when things are removed from the registry.
     * It performs the following steps:
     * 1. Checks if orphan functionality is enabled
     * 2. Either marks accessories as orphaned or removes them
     * 3. Updates configuration to reflect orphaned state
     * 4. Cleans up resources and subscriptions
     *
     * Key implementation details:
     * - Manages orphaned accessory state
     * - Handles resource cleanup
     * - Updates configuration
     * - Removes event subscriptions
     *
     * @param thing The {@link org.openhab.core.thing.Thing} that was removed
     * @throws IllegalStateException if accessory removal fails
     * @since 1.0.0
     */
    @Override
    public void removed(Thing thing) {
        logger.debug("{}Thing {} removed", LOG_PREFIX, thing.getUID());

        @Nullable
        HomekitAccessory accessory = thingAccessoryMap.get(thing.getUID());
        if (accessory != null) {
            try {
                if (orphanEnabled) {
                    // Mark the accessory as orphaned
                    accessory.setOrphaned(true);
                    logger.info(
                            "{}Thing {} was removed but keeping its HomeKit accessory to prevent controller deletion",
                            LOG_PREFIX, thing.getUID());

                    // Update configuration to reflect orphaned state
                    Optional<Map<String, Object>> configOpt = configManager.getConfiguration(thing.getUID(),
                            HomekitConfigurationManager.ConfigurationType.THING);
                    if (configOpt.isPresent()) {
                        Map<String, Object> config = new java.util.HashMap<>(configOpt.get());
                        config.put("orphaned", true);
                        configManager.updateConfiguration(thing.getUID(),
                                HomekitConfigurationManager.ConfigurationType.THING, config);
                    }
                } else {
                    // Completely remove the accessory and its characteristics
                    thingAccessoryMap.remove(thing.getUID());

                    // Remove all characteristics associated with this thing's channels
                    thing.getChannels().forEach(channel -> {
                        @Nullable
                        HomekitCharacteristic<?> characteristic = channelCharacteristicMap.remove(channel.getUID());
                        if (characteristic != null) {
                            // Remove from bidirectional map
                            characteristicChannelMap.remove(characteristic);
                            // Unsubscribe from characteristic events
                            unsubscribeFromCharacteristicEvents(characteristic);

                            // Remove any item-channel mappings
                            linkRegistry.getLinks(channel.getUID()).forEach(link -> {
                                itemChannelMap.remove(link.getItemName());
                            });
                        }
                    });

                    // Remove the accessory from its server
                    accessoryServerRegistry.getAccessoryServer(accessory.getUID()).ifPresent(server -> {
                        try {
                            server.removeAccessory(accessory);
                        } catch (HomekitAccessoryOperationException e) {
                            logger.error("{}Failed to remove accessory {}: {}", LOG_PREFIX, accessory.getUID(),
                                    e.getMessage(), e);
                        }
                    });

                    // Clean up profiles
                    thing.getChannels().forEach(channel -> {
                        channelProfiles.remove(channel.getUID());
                        channelHomekitToOpenhabProfiles.remove(channel.getUID());
                    });
                }
            } catch (Exception e) {
                logger.error("{}Failed to handle accessory for thing {}: {}", LOG_PREFIX, thing.getUID(),
                        e.getMessage(), e);
            }
        }
    }

    /**
     * Handles thing update events.
     *
     * This method processes events when things are updated in the registry.
     * It performs the following steps:
     * 1. Removes the old thing configuration
     * 2. Processes the new thing configuration
     * 3. Updates accessories and characteristics
     * 4. Reconfigures event subscriptions
     *
     * Key implementation details:
     * - Handles configuration updates
     * - Manages accessory lifecycle
     * - Updates event subscriptions
     * - Ensures thread-safe operations
     *
     * @param oldThing The old {@link org.openhab.core.thing.Thing}
     * @param newThing The new {@link org.openhab.core.thing.Thing}
     * @throws IllegalStateException if thing update fails
     * @since 1.0.0
     */
    @Override
    public void updated(Thing oldThing, Thing newThing) {
        logger.debug("{}Thing {} updated", LOG_PREFIX, newThing.getUID());

        // Remove the old thing first
        removed(oldThing);

        // Process the new thing
        processThing(newThing);
    }

    /**
     * Restores an orphaned accessory.
     *
     * This method attempts to restore an orphaned accessory when its thing becomes
     * available again.
     * It performs the following steps:
     * 1. Removes orphaned flag from accessory
     * 2. Updates configuration to remove orphaned state
     * 3. Re-subscribes to characteristic events
     * 4. Validates restoration success
     *
     * Key implementation details:
     * - Updates accessory state
     * - Manages configuration changes
     * - Re-establishes event subscriptions
     * - Handles error conditions
     *
     * @param thing The {@link org.openhab.core.thing.Thing} to restore
     *            the accessory for
     * @param orphanedAccessory The orphaned
     *            {@link org.openhab.io.homekit.api.accessory.HomekitAccessory}
     * @return true if the accessory was restored successfully, false otherwise
     * @throws IllegalStateException if restoration fails
     * @since 1.0.0
     */
    private boolean restoreOrphanedAccessory(Thing thing, HomekitAccessory orphanedAccessory) {
        try {
            // Remove orphaned flag
            orphanedAccessory.setOrphaned(false);

            // Update configuration
            Optional<Map<String, Object>> configOpt = configManager.getConfiguration(thing.getUID(),
                    HomekitConfigurationManager.ConfigurationType.THING);
            if (configOpt.isPresent()) {
                Map<String, Object> config = new java.util.HashMap<>(configOpt.get());
                config.remove("orphaned");
                configManager.updateConfiguration(thing.getUID(), HomekitConfigurationManager.ConfigurationType.THING,
                        config);
            }

            // Re-subscribe to characteristic events
            orphanedAccessory.getServices().forEach(service -> {
                service.getCharacteristics().forEach(characteristic -> {
                    subscribeToCharacteristicEvents(characteristic);
                });
            });

            logger.info("{}Successfully restored orphaned accessory for thing {}", LOG_PREFIX, thing.getUID());
            return true;
        } catch (Exception e) {
            logger.error("{}Failed to restore orphaned accessory for thing {}: {}", LOG_PREFIX, thing.getUID(),
                    e.getMessage(), e);
            return false;
        }
    }

    /**
     * Checks if a thing is orphaned.
     *
     * This method determines if a thing's accessory is in an orphaned state.
     * It performs the following checks:
     * 1. Retrieves the accessory for the thing
     * 2. Checks the orphaned state of the accessory
     * 3. Validates the accessory's configuration
     *
     * Key implementation details:
     * - Uses {@link #thingAccessoryMap} for accessory lookup
     * - Checks accessory orphaned state
     * - Validates configuration
     *
     * @param thing The {@link org.openhab.core.thing.Thing} to check
     * @return true if the thing's accessory is orphaned, false otherwise
     * @since 1.0.0
     */
    public boolean isOrphaned(Thing thing) {
        @Nullable
        HomekitAccessory accessory = thingAccessoryMap.get(thing.getUID());
        return accessory != null && accessory.isOrphaned();
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
        private Optional<ScheduledFuture<?>> scheduledTask = Optional.empty();
        private Optional<ScheduledExecutorService> executor = Optional.empty();

        public void start() {
            synchronized (lock) {
                if (executor.isEmpty()) {
                    executor = Optional
                            .of(ThreadPoolManager.getScheduledPool(HomekitBindingConstants.THREAD_POOL_NAME));
                }
                if (scheduledTask.isEmpty()) {
                    scheduledTask = Optional.of(executor.get().scheduleWithFixedDelay(this::printStatistics,
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

                // These values cannot be null since they come from a non-empty ArrayList
                // that we just created and sorted above
                long minTime = sortedTimes.get(0);
                long maxTime = sortedTimes.get(sortedTimes.size() - 1);

                logger.info(
                        "Exit Event Statistics (based on {} events):\nMean: {} ms\nStd Dev: {} ms\nMin: {} ms\nMax: {} ms \nHistogram:{}",
                        eventTimes.size(), mean, stdDev, minTime, maxTime, histogram.toString());
            }
        }
    }

    /**
     * Gets all things managed by this bridge.
     *
     * @return Collection of {@link Thing} instances
     * @since 1.0.0
     */
    public Collection<Thing> getThings() {
        @SuppressWarnings("null") // Collectors.toList() never returns null
        Collection<Thing> result = thingAccessoryMap.keySet().stream().map(thingRegistry::get).filter(Objects::nonNull)
                .collect(Collectors.toList());
        return result;
    }

    /**
     * Gets the accessory mapped to a thing.
     *
     * @param thingUID The {@link ThingUID} to get the accessory for
     * @return Optional containing the mapped {@link HomekitAccessory}, or empty if
     *         not found
     * @since 1.0.0
     */
    public Optional<HomekitAccessory> getMappedAccessory(ThingUID thingUID) {
        return Optional.ofNullable(thingAccessoryMap.get(thingUID));
    }

    /**
     * Gets all accessories managed by this bridge.
     *
     * @return Collection of {@link HomekitAccessory} instances
     * @deprecated Use {@link #getMappedAccessory(ThingUID)} instead
     * @since 1.0.0
     */
    @Deprecated
    public Collection<HomekitAccessory> getAccessories() {
        return thingAccessoryMap.values();
    }
}
