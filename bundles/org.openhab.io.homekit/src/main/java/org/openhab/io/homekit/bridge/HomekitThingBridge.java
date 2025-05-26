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

import javax.annotation.Nullable;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
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
import org.openhab.core.thing.internal.profiles.ProfileCallbackImpl;
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
import org.openhab.io.homekit.util.HomekitUID;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The HomekitThingBridge is responsible for bridging OpenHAB Things to HomeKit accessories.
 * It handles bidirectional communication between OpenHAB and HomeKit using profiles for state/command conversion.
 *
 * Key features:
 * 1. Bidirectional communication between OpenHAB and HomeKit
 * 2. Profile-based state/command conversion
 * 3. Event handling for both directions
 * 4. Configuration management
 * 5. Bridge context management
 *
 * Communication Flow:
 * 1. OpenHAB -> HomeKit:
 * - Commands/States from OpenHAB items are processed through profiles
 * - Profiles convert OpenHAB types to HomeKit types
 * - Converted values are sent to HomeKit characteristics
 *
 * 2. HomeKit -> OpenHAB:
 * - Events from HomeKit are received through the event subscriber
 * - Events are processed through profiles
 * - Profiles convert HomeKit types to OpenHAB types
 * - Converted values are sent to OpenHAB items
 *
 * Profile System:
 * - Profiles are used to convert between OpenHAB and HomeKit types
 * - Two profiles are created for each channel:
 * 1. openhabToHomekitProfile: Converts OpenHAB types to HomeKit types
 * 2. homekitToOpenhabProfile: Converts HomeKit types to OpenHAB types
 * - NoOpProfile is used as fallback when no specific profile is configured
 * - Profiles can be configured through Item-Channel link metadata
 *
 * @author openHAB Contributors
 */
@Component(service = { EventSubscriber.class, ThingRegistryChangeListener.class })
@NonNullByDefault
public class HomekitThingBridge implements EventSubscriber, ThingRegistryChangeListener {
    private static final String LOG_PREFIX = "[HomekitThingBridge] ";
    private static final String THREAD_POOL_NAME = "homekit";
    private final Logger logger = LoggerFactory.getLogger(HomekitThingBridge.class);

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

    // Maps to store relationships between Things, Channels, and HomeKit accessories/characteristics
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
     * Creates a new HomekitThingBridge instance.
     * Initializes all required services and managers.
     *
     * @param thingRegistry Registry for OpenHAB Things
     * @param itemChannelLinkRegistry Registry for Item-Channel links
     * @param channelTypeRegistry Registry for Channel types
     * @param profileFactory Factory for creating profiles
     * @param accessoryServerRegistry Registry for HomeKit accessory servers
     * @param configManager Manager for HomeKit configuration
     * @param eventPublisher Publisher for OpenHAB events
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
        Object orphanConfig = properties.get(CONFIG_ORPHAN_ENABLED);
        if (orphanConfig != null) {
            this.orphanEnabled = Boolean.parseBoolean(orphanConfig.toString());
            logger.info("{}Orphan functionality is {}", LOG_PREFIX, orphanEnabled ? "enabled" : "disabled");
        }

        // Initialize existing Things
        initializeExistingThings();

        this.peerGroup = Set.of(bridgeUID, new HomekitPeerGroupUIDImpl("openhab"),
                new HomekitPeerGroupUIDImpl("homekit"));
        this.statisticsCollector = new ExitEventStatisticsCollector();

        if (ENABLE_EXIT_EVENT_STATISTICS) {
            statisticsCollector.start();
        }
    }

    private void initializeExistingThings() {
        thingRegistry.getAll().forEach(this::processThing);
    }

    private void processThing(Thing thing) {
        // Get configuration for the Thing from the thingToAccessory mapping
        Optional<Map<String, Object>> thingConfigOpt = configManager.getConfiguration(thing.getUID(),
                HomekitConfigurationManager.ConfigurationType.THING);

        if (thingConfigOpt.isEmpty()) {
            logger.debug("{}Thing {} not configured for bridging", LOG_PREFIX, thing.getUID());
            return;
        }

        Map<String, Object> thingConfig = thingConfigOpt.get();
        if (!Boolean.TRUE.equals(thingConfig.get("bridge"))) {
            logger.debug("{}Thing {} not configured as bridge", LOG_PREFIX, thing.getUID());
            return;
        }

        // Create HomeKit accessory for the Thing
        HomekitAccessoryServer server = accessoryServerRegistry.getAvailableBridgeAccessoryServer();
        if (server == null) {
            logger.warn("{}No available bridge accessory server found for thing {}", LOG_PREFIX, thing.getUID());
            return;
        }

        try {
            HomekitAccessory accessory = createAccessoryForThing(thing, server, thingConfig);
            if (accessory != null) {
                thingAccessoryMap.put(thing.getUID(), accessory);
                processChannels(thing, accessory);
            }
        } catch (Exception e) {
            logger.error("{}Failed to create accessory for thing {}: {}", LOG_PREFIX, thing.getUID(), e.getMessage(),
                    e);
        }
    }

    private HomekitAccessory createAccessoryForThing(Thing thing, HomekitAccessoryServer server,
            Map<String, Object> thingConfig) {
        // Get accessory type from configuration
        String accessoryType = (String) thingConfig.get("accessory");
        if (accessoryType == null) {
            accessoryType = "generic"; // Default to generic if not specified
        }

        // Create accessory based on Thing type
        HomekitAccessory accessory = accessoryFactory.createAccessory(accessoryType);

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
                                            c.setValue(Json.createValue((String) value));
                                        } else if (value instanceof Number) {
                                            c.setValue(Json.createValue(((Number) value).doubleValue()));
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
            return null;
        }
        return accessory;
    }

    private void processChannels(Thing thing, HomekitAccessory accessory) {
        // Get service mappings for this thing
        Optional<Map<String, Object>> serviceMappingsOpt = configManager.getConfiguration(thing.getUID(),
                HomekitConfigurationManager.ConfigurationType.CHANNEL);

        Map<String, Object> serviceMappings = serviceMappingsOpt.orElse(Collections.emptyMap());

        for (Channel channel : thing.getChannels()) {
            String channelUID = channel.getUID().toString();

            // Try to find a matching service mapping
            Optional<Map<String, Object>> serviceConfig = findMatchingServiceConfig(channelUID, serviceMappings);
            if (serviceConfig.isEmpty()) {
                continue;
            }

            Map<String, Object> config = serviceConfig.get();
            String serviceTag = (String) config.get("serviceTag");
            if (serviceTag == null) {
                continue;
            }

            try {
                // Create or get the service
                HomekitService service = accessory.getService(serviceTag).orElseGet(() -> {
                    HomekitService newService = serviceFactory.createService(serviceTag, accessory);
                    accessory.addService(newService);
                    return newService;
                });

                // Process characteristics for this service
                @SuppressWarnings("unchecked")
                Map<String, Object> characteristics = (Map<String, Object>) config.get("characteristics");
                if (characteristics != null) {
                    processCharacteristics(channel, service, characteristics);
                }
            } catch (Exception e) {
                logger.error("{}Failed to process channel {}: {}", LOG_PREFIX, channelUID, e.getMessage(), e);
            }
        }
    }

    private Optional<Map<String, Object>> findMatchingServiceConfig(String channelUID,
            Map<String, Object> serviceMappings) {
        // First try exact match
        if (serviceMappings.containsKey(channelUID)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> config = (Map<String, Object>) serviceMappings.get(channelUID);
            return Optional.of(config);
        }

        // Then try pattern matching
        for (Map.Entry<String, Object> entry : serviceMappings.entrySet()) {
            String pattern = entry.getKey();
            if (pattern.contains("*")) {
                String regex = pattern.replace("*", ".*");
                if (channelUID.matches(regex)) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> config = (Map<String, Object>) entry.getValue();
                    return Optional.of(config);
                }
            }
        }

        return Optional.empty();
    }

    private void processCharacteristics(Channel channel, HomekitService service, Map<String, Object> characteristics) {
        for (Map.Entry<String, Object> entry : characteristics.entrySet()) {
            String characteristicType = entry.getKey();
            @SuppressWarnings("unchecked")
            Map<String, Object> config = (Map<String, Object>) entry.getValue();

            try {
                HomekitCharacteristic<?> characteristic = characteristicFactory.createCharacteristic(characteristicType,
                        service);
                if (characteristic != null) {
                    // Apply characteristic configuration
                    if (config.containsKey("inverted")
                            && characteristic instanceof AbstractHomekitCharacteristic<?> abstractCharacteristic) {
                        // Set the inverted property through the characteristic's configuration
                        JsonObjectBuilder builder = Json.createObjectBuilder();
                        builder.add("inverted", (Boolean) config.get("inverted"));
                        abstractCharacteristic.setValue(builder.build());
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
                }
            } catch (Exception e) {
                logger.error("{}Failed to create characteristic {} for channel {}: {}", LOG_PREFIX, characteristicType,
                        channel.getUID(), e.getMessage(), e);
            }
        }
    }

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

    private void unsubscribeFromCharacteristicEvents(HomekitCharacteristic<?> characteristic) {
        try {
            // Find and remove all subscriptions for this characteristic
            eventSubscriptions.removeIf(subscription -> {
                if (subscription.getPublisherUID().equals(characteristic.getUID())) {
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

    private void handleCharacteristicEvent(HomekitCharacteristic<?> characteristic, HomekitEvent event) {
        if (event instanceof HomekitCharacteristicChangedEvent changedEvent) {
            try {
                // Find the channel for this characteristic using the bidirectional map
                ChannelUID channelUID = characteristicChannelMap.get(characteristic);
                if (channelUID != null) {
                    // Check if event is from peer group
                    if (event.getMetadata().isFromPeerGroup(peerGroup)) {
                        logger.debug("{}Ignoring Homekit event from peer group: {}", LOG_PREFIX,
                                event.getMetadata().getImmediateOrigin());
                        return;
                    }

                    // Use the HomeKit -> OpenHAB profile
                    Profile profile = channelHomekitToOpenhabProfiles.get(channelUID);
                    if (profile instanceof StateProfile stateProfile) {
                        State newState = characteristic.toState(changedEvent.getNewValue().orElse(null));
                        if (newState != null) {
                            // Store the exit event
                            String itemName = itemChannelLinkRegistry.getLinks(channelUID).stream().findFirst()
                                    .map(ItemChannelLink::getItemName).orElse(null);
                            if (itemName != null) {
                                exitEvents.put(itemName, new ExitEvent(newState, event.getMetadata()));
                            }
                            stateProfile.onStateUpdateFromHandler(newState);
                        }
                    } else {
                        // If no profile is used, directly handle the state
                        State newState = characteristic.toState(changedEvent.getNewValue().orElse(null));
                        if (newState != null) {
                            String itemName = itemChannelLinkRegistry.getLinks(channelUID).stream().findFirst()
                                    .map(ItemChannelLink::getItemName).orElse(null);
                            if (itemName != null) {
                                exitEvents.put(itemName, new ExitEvent(newState, event.getMetadata()));
                                eventPublisher.post(ItemEventFactory.createStateEvent(itemName, newState));
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("{}Failed to handle characteristic event: {}", LOG_PREFIX, e.getMessage(), e);
            }
        }
    }

    @Override
    public Set<String> getSubscribedEventTypes() {
        return Set.of(ItemCommandEvent.TYPE, ItemStateEvent.TYPE);
    }

    @Override
    public void receive(Event event) {
        if (event instanceof ItemCommandEvent commandEvent) {
            handleItemCommand(commandEvent);
        } else if (event instanceof ItemStateEvent stateEvent) {
            handleItemState(stateEvent);
        }
    }

    private void handleItemCommand(ItemCommandEvent event) {
        String itemName = event.getItemName();
        Command command = event.getItemCommand();

        // Find the Channel linked to this Item
        ChannelUID channelUID = itemChannelMap.get(itemName);
        if (channelUID == null) {
            return;
        }

        // Find the Characteristic for this Channel
        HomekitCharacteristic<?> characteristic = channelCharacteristicMap.get(channelUID);
        if (characteristic == null) {
            return;
        }

        // Check if we have a profile for this channel
        Profile profile = channelProfiles.get(channelUID);
        if (profile != null) {
            // Use the profile to handle the command
            if (profile instanceof StateProfile stateProfile) {
                stateProfile.onCommandFromItem(command);
            }
        } else {
            // Direct communication - convert and send event
            try {
                HomekitCharacteristicUpdateEvent updateEvent = new HomekitCharacteristicUpdateEvent((UID) bridgeUID,
                        (UID) characteristic.getUID(), characteristic, characteristic.toValueJson((State) command),
                        null, Map.of(), new HomekitEventMetadata(bridgeUID, null, bridgeUID, Set.of()));
                eventManager.publishEvent(updateEvent);
            } catch (Exception e) {
                logger.error("{}Failed to handle command for item {}: {}", LOG_PREFIX, itemName, e.getMessage(), e);
            }
        }
    }

    private void handleItemState(ItemStateEvent event) {
        String itemName = event.getItemName();
        State state = event.getItemState();

        // Check if this is a correlated event (originated from HomeKit)
        if (event.getSource() != null && event.getSource().equals(bridgeUID.toString())) {
            logger.debug("{}Dropping correlated event for item {}", LOG_PREFIX, itemName);
            return;
        }

        // Find the Channel linked to this Item
        ChannelUID channelUID = itemChannelMap.get(itemName);
        if (channelUID == null) {
            return;
        }

        // Find the Characteristic for this Channel
        HomekitCharacteristic<?> characteristic = channelCharacteristicMap.get(channelUID);
        if (characteristic == null) {
            return;
        }

        // Clean up expired exit events
        exitEvents.entrySet().removeIf(entry -> entry.getValue().isExpired());

        // Check if we have a profile for this channel
        Profile profile = channelProfiles.get(channelUID);
        if (profile != null) {
            // Use the profile to handle the state
            if (profile instanceof StateProfile stateProfile) {
                stateProfile.onStateUpdateFromItem(state);
            }
        } else {
            // Direct communication - convert and send event
            try {
                ExitEvent exitEvent = exitEvents.get(itemName);
                Optional.ofNullable(exitEvent).filter(e -> !e.isExpired()).ifPresent(e -> {
                    if (statesEqual(e.getState(), state)) {
                        logger.debug("{}Processing correlated state change for item: {}", LOG_PREFIX, itemName);
                        if (ENABLE_EXIT_EVENT_STATISTICS) {
                            statisticsCollector.recordEvent(System.currentTimeMillis() - e.getTimestamp());
                        }
                        HomekitCharacteristicUpdateEvent updateEvent = new HomekitCharacteristicUpdateEvent(
                                (UID) bridgeUID, (UID) characteristic.getUID(), characteristic,
                                characteristic.toValueJson(e.getState()), characteristic.toValueJson(state), Map.of(),
                                e.getMetadata());
                        eventManager.publishEvent(updateEvent);
                        exitEvents.remove(itemName);
                    }
                });

                // Handle uncorrelated state change
                if (exitEvent == null) {
                    logger.debug("{}Processing new state change for item: {}", LOG_PREFIX, itemName);
                    HomekitCharacteristicUpdateEvent updateEvent = new HomekitCharacteristicUpdateEvent((UID) bridgeUID,
                            (UID) characteristic.getUID(), characteristic, characteristic.toValueJson(state), null,
                            Map.of(), new HomekitEventMetadata(bridgeUID, null, bridgeUID, peerGroup));
                    eventManager.publishEvent(updateEvent);
                }
            } catch (Exception e) {
                logger.error("{}Failed to handle state update for item {}: {}", LOG_PREFIX, itemName, e.getMessage(),
                        e);
            }
        }
    }

    private boolean statesEqual(State state1, State state2) {
        if (state1 == state2)
            return true;
        if (state1 == null || state2 == null)
            return false;
        return state1.equals(state2);
    }

    /**
     * Creates profiles for a channel and its associated Item-Channel link.
     * Creates two profiles:
     * 1. For OpenHAB -> HomeKit direction
     * 2. For HomeKit -> OpenHAB direction
     *
     * @param channel The channel to create profiles for
     * @param link The Item-Channel link associated with the channel
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
                    return ThreadPoolManager.getScheduledPool(THREAD_POOL_NAME);
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
                    new ProfileCallbackImpl(eventPublisher, safeCaller, itemStateConverter, link, thingRegistry::get,
                            this::getItem, this::toAcceptedCommand),
                    context);

            // public ProfileCallbackImpl(EventPublisher eventPublisher, SafeCaller safeCaller,
            // ItemStateConverter itemStateConverter, ItemChannelLink link,
            // Function<ThingUID, @Nullable Thing> thingProvider, Function<String, @Nullable Item> itemProvider,
            // AcceptedTypeConverter acceptedTypeConverter) {

            channelHomekitToOpenhabProfiles.put(channel.getUID(), homekitToOpenhabProfile);
        }

        // Store both profiles
        channelProfiles.put(channel.getUID(), openhabToHomekitProfile);
    }

    private @Nullable Item getItem(final String itemName) {
        return itemRegistry.get(itemName);
    }

    public @Nullable Command toAcceptedCommand(Command originalType, @Nullable Channel channel, @Nullable Item item) {
        if (item == null || channel == null) {
            logger.warn("Trying to convert types for non-existing channel or item, discarding command.");
            return null;
        }
        String channelAcceptedItemType = channel.getAcceptedItemType();

        if (channelAcceptedItemType == null) {
            return originalType;
        }

        Command uomCommand = fixUoM(originalType, channel, item);
        if (uomCommand != null) {
            return uomCommand;
        }

        // handle HSBType/PercentType
        if (CoreItemFactory.DIMMER.equals(channelAcceptedItemType) && originalType instanceof HSBType hsb) {
            return hsb.as(PercentType.class);
        }

        // check for other cases if the type is acceptable
        List<Class<? extends Command>> acceptedTypes = acceptedCommandTypeMap.get(channelAcceptedItemType);
        if (acceptedTypes == null || acceptedTypes.contains(originalType.getClass())) {
            return originalType;
        } else if (acceptedTypes.contains(PercentType.class) && originalType instanceof State state
                && PercentType.class.isAssignableFrom(originalType.getClass())) {
            return state.as(PercentType.class);
        } else if (acceptedTypes.contains(OnOffType.class) && originalType instanceof State state
                && PercentType.class.isAssignableFrom(originalType.getClass())) {
            return state.as(OnOffType.class);
        } else {
            logger.debug("Received not accepted type '{}' for channel '{}'", originalType.getClass().getSimpleName(),
                    channel.getUID());
            return null;
        }
    }

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

    @SuppressWarnings("unchecked")
    private @Nullable <T extends Type> T fixUoM(@Nullable T originalType, Channel channel, Item item) {
        String channelAcceptedItemType = channel.getAcceptedItemType();

        if (channelAcceptedItemType == null) {
            return originalType;
        }

        // handle Number-Channels for backward compatibility
        if (CoreItemFactory.NUMBER.equals(channelAcceptedItemType)
                && originalType instanceof QuantityType<?> quantityType) {
            // strip unit from QuantityType for channels that accept plain number
            return (T) new DecimalType(quantityType.toBigDecimal());
        }

        String itemDimension = ItemUtil.getItemTypeExtension(item.getType());
        String channelDimension = ItemUtil.getItemTypeExtension(channelAcceptedItemType);

        if (originalType instanceof DecimalType decimalType && channelDimension != null
                && channelDimension.equals(itemDimension)) {
            // Add unit from item to DecimalType when dimensions are equal
            Unit<?> unit = Objects.requireNonNull(((NumberItem) item).getUnit());
            return (T) new QuantityType<>(decimalType.toBigDecimal(), unit);
        }
        return null;
    }

    @Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC)
    protected void addItemFactory(ItemFactory itemFactory) {
        itemFactories.add(itemFactory);
        calculateAcceptedTypes();
    }

    protected void removeItemFactory(ItemFactory itemFactory) {
        itemFactories.remove(itemFactory);
        calculateAcceptedTypes();
    }

    /**
     * Handles commands from OpenHAB to HomeKit.
     * Converts the command to a HomeKit characteristic value.
     *
     * @param channelUID The channel UID
     * @param command The command to handle
     */
    private void handleProfileCommand(ChannelUID channelUID, Command command) {
        HomekitCharacteristic<?> characteristic = channelCharacteristicMap.get(channelUID);
        if (characteristic != null) {
            try {
                HomekitCharacteristicUpdateEvent updateEvent = new HomekitCharacteristicUpdateEvent((UID) bridgeUID,
                        (UID) characteristic.getUID(), characteristic, characteristic.toValueJson((State) command),
                        null, Map.of(), new HomekitEventMetadata(bridgeUID, null, bridgeUID, Set.of()));
                eventManager.publishEvent(updateEvent);
            } catch (Exception e) {
                logger.error("{}Failed to handle command for channel {}: {}", LOG_PREFIX, channelUID, e.getMessage(),
                        e);
            }
        }
    }

    /**
     * Handles state updates from OpenHAB to HomeKit.
     * Converts the state to a HomeKit characteristic value.
     *
     * @param channelUID The channel UID
     * @param state The state to handle
     */
    private void handleProfileState(ChannelUID channelUID, State state) {
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
     * Deactivates the bridge.
     * Cleans up all profiles and resources.
     */
    public void deactivate() {
        if (ENABLE_EXIT_EVENT_STATISTICS) {
            statisticsCollector.stop();
        }
        // Unsubscribe from all characteristic events
        eventSubscriptions.forEach(subscription -> {
            try {
                eventManager.unsubscribe(subscription.getEventType(), subscription.getPublisherUID(),
                        subscription.getSubscriber());
            } catch (Exception e) {
                logger.error("{}Failed to unsubscribe from event: {}", LOG_PREFIX, e.getMessage(), e);
            }
        });
        eventSubscriptions.clear();

        // Remove accessories and clean up
        thingAccessoryMap.values().forEach(accessory -> {
            try {
                HomekitAccessoryServer server = (HomekitAccessoryServer) accessoryServerRegistry
                        .getAccessoryServer(accessory.getUID());
                if (server != null) {
                    try {
                        server.removeAccessory(accessory);
                    } catch (HomekitAccessoryOperationException e) {
                        logger.error("{}Failed to remove accessory {}: {}", LOG_PREFIX, accessory.getUID(),
                                e.getMessage(), e);
                    }
                }
                ;
            } catch (Exception e) {
                logger.error("{}Failed to access server for accessory {}: {}", LOG_PREFIX, accessory.getUID(),
                        e.getMessage(), e);
            }
        });

        thingAccessoryMap.clear();
        channelCharacteristicMap.clear();
        characteristicChannelMap.clear();
        itemChannelMap.clear();

        // Clean up both profile maps
        channelProfiles.clear();
        channelHomekitToOpenhabProfiles.clear();
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
        public @NonNull ProfileTypeUID getProfileTypeUID() {
            return new ProfileTypeUID("homekit", "noop");
        }

        @Override
        public void onStateUpdateFromItem(@NonNull State state) {
            // Forward state to handler
            callback.sendUpdate(state);
        }

        @Override
        public void onStateUpdateFromHandler(@NonNull State state) {
            // Forward state to item
            callback.sendUpdate(state);
        }

        @Override
        public void onCommandFromItem(@NonNull Command command) {
            // Forward command to handler
            callback.handleCommand(command);
        }

        @Override
        public void onCommandFromHandler(@NonNull Command command) {
            // Forward command to item
            callback.handleCommand(command);
        }
    }

    @Override
    public void added(Thing thing) {
        logger.debug("{}Thing {} added", LOG_PREFIX, thing.getUID());

        if (orphanEnabled) {
            // Check if this is a restoration of an orphaned accessory
            HomekitAccessory existingAccessory = thingAccessoryMap.get(thing.getUID());
            if (existingAccessory != null && existingAccessory.isOrphaned()) {
                if (restoreOrphanedAccessory(thing, existingAccessory)) {
                    logger.info("{}Successfully restored orphaned accessory for thing {}", LOG_PREFIX, thing.getUID());
                    return;
                }
            }
        }

        // Process the new thing if it's not a restoration or if orphan functionality is disabled
        processThing(thing);
    }

    @Override
    public void removed(Thing thing) {
        logger.debug("{}Thing {} removed", LOG_PREFIX, thing.getUID());

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
                    HomekitAccessoryServer server = (HomekitAccessoryServer) accessoryServerRegistry
                            .getAccessoryServer(accessory.getUID());
                    if (server != null) {
                        try {
                            server.removeAccessory(accessory);
                        } catch (HomekitAccessoryOperationException e) {
                            logger.error("{}Failed to remove accessory {}: {}", LOG_PREFIX, accessory.getUID(),
                                    e.getMessage(), e);
                        }
                    }

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
     * Checks if an accessory is orphaned.
     *
     * @param thing The thing to check
     * @return true if the accessory is orphaned, false otherwise
     */
    public boolean isOrphaned(Thing thing) {
        HomekitAccessory accessory = thingAccessoryMap.get(thing.getUID());
        return accessory != null && accessory.isOrphaned();
    }

    @Override
    public void updated(Thing oldThing, Thing newThing) {
        logger.debug("{}Thing {} updated", LOG_PREFIX, newThing.getUID());

        // Remove the old thing first
        removed(oldThing);

        // Process the new thing
        processThing(newThing);
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

    /**
     * Gets all things managed by this bridge.
     * 
     * @return Collection of all things
     */
    public Collection<Thing> getThings() {
        return thingAccessoryMap.keySet().stream().map(thingRegistry::get).filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Gets the HomeKit accessory mapped to a specific thing.
     * 
     * @param thingUID The UID of the thing
     * @return Optional containing the mapped accessory if found
     */
    public Optional<HomekitAccessory> getMappedAccessory(ThingUID thingUID) {
        return Optional.ofNullable(thingAccessoryMap.get(thingUID));
    }

    /**
     * Gets all HomeKit accessories managed by this bridge.
     * This method is kept for backward compatibility.
     * 
     * @return Collection of all accessories
     * @deprecated Use getThings() and getMappedAccessory() instead
     */
    @Deprecated
    public Collection<HomekitAccessory> getAccessories() {
        return thingAccessoryMap.values();
    }
}
