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

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.common.registry.RegistryChangeListener;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.events.ItemCommandEvent;
import org.openhab.core.items.events.ItemEventFactory;
import org.openhab.core.items.events.ItemStateEvent;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingRegistryChangeListener;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.events.ThingEventFactory;
import org.openhab.core.thing.link.ItemChannelLink;
import org.openhab.core.thing.link.ItemChannelLinkRegistry;
import org.openhab.core.thing.profiles.Profile;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.ProfileContext;
import org.openhab.core.thing.profiles.ProfileFactory;
import org.openhab.core.thing.profiles.ProfileTypeUID;
import org.openhab.core.thing.profiles.StateProfile;
import org.openhab.core.thing.profiles.TriggerProfile;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeRegistry;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.TimeSeries;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.server.HomekitAccessoryServer;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristic;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.config.HomekitConfigurationManager;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.event.model.characteristic.HomekitCharacteristicChangedEvent;
import org.openhab.io.homekit.event.model.characteristic.HomekitCharacteristicUpdateEvent;
import org.openhab.io.homekit.util.HomekitUID;
import org.openhab.io.homekit.api.event.HomekitEvent;
import org.openhab.io.homekit.api.event.HomekitEventSubscriber;
import org.openhab.io.homekit.api.event.HomekitEventType;
import org.openhab.io.homekit.api.factory.HomekitAccessoryFactory;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.registry.HomekitAccessoryServerRegistry;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openhab.core.thing.internal.profiles.ProfileCallbackImpl;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.openhab.core.events.EventSubscriber;
import java.util.HashSet;
import org.openhab.io.homekit.event.core.HomekitEventMetadata;
import org.openhab.io.homekit.event.core.HomekitEventSubscription;
import org.openhab.core.config.core.Configuration;
import java.util.concurrent.ScheduledExecutorService;

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
 *    - Commands/States from OpenHAB items are processed through profiles
 *    - Profiles convert OpenHAB types to HomeKit types
 *    - Converted values are sent to HomeKit characteristics
 *
 * 2. HomeKit -> OpenHAB:
 *    - Events from HomeKit are received through the event subscriber
 *    - Events are processed through profiles
 *    - Profiles convert HomeKit types to OpenHAB types
 *    - Converted values are sent to OpenHAB items
 *
 * Profile System:
 * - Profiles are used to convert between OpenHAB and HomeKit types
 * - Two profiles are created for each channel:
 *   1. openhabToHomekitProfile: Converts OpenHAB types to HomeKit types
 *   2. homekitToOpenhabProfile: Converts HomeKit types to OpenHAB types
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

    // Core OpenHAB services
    private final ThingRegistry thingRegistry;
    private final ItemChannelLinkRegistry itemChannelLinkRegistry;
    private final ChannelTypeRegistry channelTypeRegistry;
    private final ProfileFactory profileFactory;
    private final HomekitAccessoryServerRegistry accessoryServerRegistry;
    private final HomekitConfigurationManager configManager;
    private final EventPublisher eventPublisher;
    private final HomekitAccessoryFactory accessoryFactory;
    private final HomekitCharacteristicFactory characteristicFactory;
    private final ItemChannelLinkRegistry linkRegistry;
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
            @Reference ItemChannelLinkRegistry itemChannelLinkRegistry,
            @Reference ChannelTypeRegistry channelTypeRegistry,
            @Reference ProfileFactory profileFactory,
            @Reference HomekitAccessoryServerRegistry accessoryServerRegistry,
            @Reference HomekitConfigurationManager configManager,
            @Reference EventPublisher eventPublisher,
            @Reference HomekitAccessoryFactory accessoryFactory,
            @Reference HomekitCharacteristicFactory characteristicFactory,
            @Reference ItemChannelLinkRegistry linkRegistry,
            @Reference HomekitEventManager eventManager) {
        this.thingRegistry = thingRegistry;
        this.itemChannelLinkRegistry = itemChannelLinkRegistry;
        this.channelTypeRegistry = channelTypeRegistry;
        this.profileFactory = profileFactory;
        this.accessoryServerRegistry = accessoryServerRegistry;
        this.configManager = configManager;
        this.eventPublisher = eventPublisher;
        this.accessoryFactory = accessoryFactory;
        this.characteristicFactory = characteristicFactory;
        this.linkRegistry = linkRegistry;
        this.eventManager = eventManager;
        // Initialize existing Things
        initializeExistingThings();
    }

    private void initializeExistingThings() {
        thingRegistry.getAll().forEach(this::processThing);
    }

    private void processThing(Thing thing) {
        // Get configuration for the Thing
        Optional<Map<String, Object>> configOpt = configManager.getConfiguration(
                thing.getUID().toString(), 
                HomekitConfigurationManager.ConfigurationType.THING);

        if (configOpt.isEmpty() || !Boolean.TRUE.equals(configOpt.get().get("bridge"))) {
            logger.debug("{}Thing {} not configured for bridging", LOG_PREFIX, thing.getUID());
            return;
        }

        // Create HomeKit accessory for the Thing
        HomekitAccessoryServer server = accessoryServerRegistry.getAvailableBridgeAccessoryServer();
        if (server == null) {
            logger.warn("{}No available bridge accessory server found for thing {}", LOG_PREFIX, thing.getUID());
            return;
        }

        try {
            HomekitAccessory accessory = createAccessoryForThing(thing, server);
            if (accessory != null) {
                thingAccessoryMap.put(thing.getUID(), accessory);
                processChannels(thing, accessory);
            }
        } catch (Exception e) {
            logger.error("{}Failed to create accessory for thing {}: {}", LOG_PREFIX, thing.getUID(), e.getMessage(), e);
        }
    }

    private HomekitAccessory createAccessoryForThing(Thing thing, HomekitAccessoryServer server) {
        // Create accessory based on Thing type
        HomekitAccessory accessory = accessoryFactory.createAccessory("generic");
        // TODO : Set additonal parameters on the accessory
        accessory.assignToServer(server);
        return accessory;
    }

    private void processChannels(Thing thing, HomekitAccessory accessory) {
        for (Channel channel : thing.getChannels()) {
            // Get configuration for the Channel
            Optional<Map<String, Object>> configOpt = configManager.getConfiguration(
                    channel.getUID().toString(), 
                    HomekitConfigurationManager.ConfigurationType.CHANNEL);

            if (configOpt.isEmpty()) {
                continue;
            }

            // Create characteristic for the Channel
            String characteristicType = (String) configOpt.get().get("characteristic");
            if (characteristicType != null) {
                try {
                    // Get the service from the accessory
                    Optional<HomekitService> serviceOpt = accessory.getPrimaryService();
                    if (serviceOpt.isPresent()) {
                        HomekitService service = serviceOpt.get();
                        HomekitCharacteristic<?> characteristic = characteristicFactory
                                .createCharacteristic(characteristicType, service);
                        if (characteristic != null) {
                            // Add bidirectional mapping
                            channelCharacteristicMap.put(channel.getUID(), characteristic);
                            characteristicChannelMap.put(characteristic, channel.getUID());
                            service.addCharacteristic(characteristic);

                            // Subscribe to characteristic events
                            subscribeToCharacteristicEvents(characteristic);

                            // Find linked Item and create profiles
                            linkRegistry.getLinks(channel.getUID()).forEach(link -> {
                                itemChannelMap.put(link.getItemName(), channel.getUID());
                                // Create profiles for this channel and link
                                createProfileForChannel(channel, link);
                            });
                        }
                    }
                } catch (Exception e) {
                    logger.error("{}Failed to create characteristic for channel {}: {}", 
                            LOG_PREFIX, channel.getUID(), e.getMessage(), e);
                }
            }
        }
    }

    private void subscribeToCharacteristicEvents(HomekitCharacteristic<?> characteristic) {
        try {
           
            // Subscribe to value changes
            eventSubscriptions.add(eventManager.subscribe(
                HomekitEventType.CHARACTERISTIC_VALUE_CHANGED,
                characteristic.getUID(),
                bridgeUID,
                (event) -> handleCharacteristicEvent(characteristic, event)
            ));


            // Subscribe to state changes
            eventSubscriptions.add(eventManager.subscribe(
                HomekitEventType.CHARACTERISTIC_STATE_CHANGED,
                characteristic.getUID(),
                bridgeUID,
                (event) -> handleCharacteristicEvent(characteristic, event)
            ));

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
                    eventManager.unsubscribe(subscription.getEventType(), 
                        subscription.getPublisherUID(), 
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
                    // Use the HomeKit -> OpenHAB profile
                    Profile profile = channelHomekitToOpenhabProfiles.get(channelUID);
                    if (profile instanceof StateProfile stateProfile) {
                        State newState = characteristic.toState(changedEvent.getNewValue().orElse(null));
                        if (newState != null) {
                            stateProfile.onStateUpdateFromHandler(newState);
                        }
                    } else {
                        // If no profile is used, directly handle the state
                        State newState = characteristic.toState(changedEvent.getNewValue().orElse(null));
                        if (newState != null) {
                            eventPublisher.post(ItemEventFactory.createStateEvent(
                                    itemChannelLinkRegistry.getLinks(channelUID).stream()
                                            .findFirst()
                                            .map(ItemChannelLink::getItemName)
                                            .orElse(null),
                                    newState,
                                    HomekitThingBridge.class.getCanonicalName()));
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("{}Failed to handle characteristic event: {}", LOG_PREFIX, e.getMessage(), e);
            }
        }
    }

    private void publishCharacteristicUpdate(HomekitCharacteristic<?> characteristic, Object value) {
        try {
            HomekitCharacteristicUpdateEvent event = new HomekitCharacteristicUpdateEvent(
                bridgeUID,
                characteristic.getUID(),
                characteristic,
                characteristic.toValueJson(value),
                null,
                Map.of(),
                new HomekitEventMetadata(bridgeUID, null, bridgeUID, Set.of())
            );
            eventManager.publishEvent(event);
            logger.debug("{}Published update for characteristic {}: {}", LOG_PREFIX, characteristic.getUID(), value);
        } catch (Exception e) {
            logger.error("{}Failed to publish characteristic update: {}", LOG_PREFIX, e.getMessage(), e);
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
        Command command = event.getCommand();

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
                Object homekitValue = characteristic.fromCommand(command);
                HomekitCharacteristicUpdateEvent updateEvent = new HomekitCharacteristicUpdateEvent(
                    bridgeUID,
                    characteristic.getUID(),
                    characteristic,
                    characteristic.toValueJson(homekitValue),
                    null,
                    Map.of(),
                    new HomekitEventMetadata(bridgeUID, null, bridgeUID, Set.of())
                );
                eventManager.publishEvent(updateEvent);
            } catch (Exception e) {
                logger.error("{}Failed to handle command for item {}: {}", 
                        LOG_PREFIX, itemName, e.getMessage(), e);
            }
        }
    }

    private void handleItemState(ItemStateEvent event) {
        String itemName = event.getItemName();
        State state = event.getItemState();

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
            // Use the profile to handle the state
            if (profile instanceof StateProfile stateProfile) {
                stateProfile.onStateUpdateFromItem(state);
            }
        } else {
            // Direct communication - convert and send event
            try {
                Object homekitValue = characteristic.fromState(state);
                HomekitCharacteristicUpdateEvent updateEvent = new HomekitCharacteristicUpdateEvent(
                    bridgeUID,
                    characteristic.getUID(),
                    characteristic,
                    characteristic.toValueJson(homekitValue),
                    null,
                    Map.of(),
                    new HomekitEventMetadata(bridgeUID, null, bridgeUID, Set.of())
                );
                eventManager.publishEvent(updateEvent);
            } catch (Exception e) {
                logger.error("{}Failed to handle state update for item {}: {}", 
                        LOG_PREFIX, itemName, e.getMessage(), e);
            }
        }
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
        Profile homekitToOpenhabProfile = profileFactory.createProfile(profileTypeUID, new ProfileCallbackImpl(channel.getUID()), context);
        channelHomekitToOpenhabProfiles.put(channel.getUID(), homekitToOpenhabProfile);
    }

        // Store both profiles
        channelProfiles.put(channel.getUID(), openhabToHomekitProfile);

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
                Object homekitValue = characteristic.fromCommand(command);
                HomekitCharacteristicUpdateEvent updateEvent = new HomekitCharacteristicUpdateEvent(
                    bridgeUID,
                    characteristic.getUID(),
                    characteristic,
                    characteristic.toValueJson(homekitValue),
                    null,
                    Map.of(),
                    new HomekitEventMetadata(bridgeUID, null, bridgeUID, Set.of())
                );
                eventManager.publishEvent(updateEvent);
            } catch (Exception e) {
                logger.error("{}Failed to handle command for channel {}: {}", 
                        LOG_PREFIX, channelUID, e.getMessage(), e);
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
                Object homekitValue = characteristic.fromState(state);
                HomekitCharacteristicUpdateEvent updateEvent = new HomekitCharacteristicUpdateEvent(
                    bridgeUID,
                    characteristic.getUID(),
                    characteristic,
                    characteristic.toValueJson(homekitValue),
                    null,
                    Map.of(),
                    new HomekitEventMetadata(bridgeUID, null, bridgeUID, Set.of())
                );
                eventManager.publishEvent(updateEvent);
            } catch (Exception e) {
                logger.error("{}Failed to handle state update for channel {}: {}", 
                        LOG_PREFIX, channelUID, e.getMessage(), e);
            }
        }
    }

    /**
     * Handles triggers from OpenHAB to HomeKit.
     * Converts the trigger to a HomeKit characteristic value.
     *
     * @param channelUID The channel UID
     * @param event The trigger event to handle
     */
    private void handleProfileTrigger(ChannelUID channelUID, String event) {
        // TODO : Implement this
    }

    /**
     * Deactivates the bridge.
     * Cleans up all profiles and resources.
     */
    public void deactivate() {
        // Unsubscribe from all characteristic events
        eventSubscriptions.forEach(subscription -> {
            try {
                eventManager.unsubscribe(subscription.getEventType(), 
                    subscription.getPublisherUID(), 
                    subscription.getSubscriber());
            } catch (Exception e) {
                logger.error("{}Failed to unsubscribe from event: {}", LOG_PREFIX, e.getMessage(), e);
            }
        });
        eventSubscriptions.clear();

        // Remove accessories and clean up
        thingAccessoryMap.values().forEach(accessory -> {
            try {
                accessory.getServer().ifPresent(server -> server.removeAccessory(accessory));
            } catch (Exception e) {
                logger.error("{}Failed to remove accessory {}: {}", 
                        LOG_PREFIX, accessory.getUID(), e.getMessage(), e);
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
        // No action needed here as things are handled during initialization
        // The thing will be processed by initializeExistingThings() if it's a bridge
        logger.debug("{}Thing {} added", LOG_PREFIX, thing.getUID());
    }

    @Override
    public void removed(Thing thing) {
        logger.debug("{}Thing {} removed", LOG_PREFIX, thing.getUID());
        
        // Remove the accessory and its characteristics
        HomekitAccessory accessory = thingAccessoryMap.remove(thing.getUID());
        if (accessory != null) {
            try {
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
                accessory.getServer().ifPresent(server -> server.removeAccessory(accessory));
            } catch (Exception e) {
                logger.error("{}Failed to remove accessory for thing {}: {}", 
                        LOG_PREFIX, thing.getUID(), e.getMessage(), e);
            }
        }

        // Clean up profiles
        thing.getChannels().forEach(channel -> {
            channelProfiles.remove(channel.getUID());
            channelHomekitToOpenhabProfiles.remove(channel.getUID());
        });
    }

    @Override
    public void updated(Thing oldThing, Thing newThing) {
        logger.debug("{}Thing {} updated", LOG_PREFIX, newThing.getUID());
        
        // Remove the old thing first
        removed(oldThing);
        
        // Process the new thing
        processThing(newThing);
    }
} 