package org.openhab.io.homekit.handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.type.ChannelGroupType;
import org.openhab.core.thing.type.ChannelGroupTypeUID;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.io.homekit.HomekitBindingConstants;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristic;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.factory.HomekitServiceFactory;
import org.openhab.io.homekit.api.registry.HomekitAccessoryRegistry;
import org.openhab.io.homekit.api.registry.HomekitAccessoryServerRegistry;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.exception.HomekitException;
import org.openhab.io.homekit.provider.HomekitChannelGroupTypeProvider;
import org.openhab.io.homekit.provider.HomekitChannelTypeProvider;
import org.openhab.io.homekit.provider.HomekitThingTypeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for HomeKit accessory things, managing the lifecycle and state of HomeKit accessories.
 *
 * This class extends {@link AbstractHomekitHandler} to provide specific functionality for managing HomeKit accessories.
 * It handles the creation and management of channels for HomeKit services and characteristics, ensuring proper
 * synchronization between OpenHAB items and HomeKit accessories.
 *
 * Key responsibilities include:
 * - Initializing and managing channels for HomeKit services and characteristics
 * - Handling channel group creation and management
 * - Validating and maintaining accessory state
 * - Managing the lifecycle of HomeKit accessories
 *
 * The handler integrates with:
 * - {@link HomekitServiceFactory} for service creation and management
 * - {@link HomekitCharacteristicFactory} for characteristic creation and management
 * - {@link HomekitChannelGroupTypeProvider} for channel group type validation and management
 * - {@link HomekitAccessoryRegistry} for accessory registration
 * - {@link HomekitAccessoryServerRegistry} for server instance management
 * - {@link HomekitEventManager} for event handling
 *
 * @author Karel Goderis - Initial Contribution
 * @since 1.0
 */
@NonNullByDefault
public class HomekitAccessoryThingHandler extends AbstractHomekitHandler {
    // ========== Log Message Prefixes ==========
    private static final String LOG_PREFIX = "HomeKit Accessory Handler: ";
    private static final String LOG_INIT = LOG_PREFIX + "Initialization - ";
    private static final String LOG_CHANNEL = LOG_PREFIX + "Channel - ";
    private static final String LOG_WARN = LOG_PREFIX + "Warning - ";

    /** Logger instance for this class */
    private final Logger logger = LoggerFactory.getLogger(HomekitAccessoryThingHandler.class);

    /** Factory for creating HomeKit services */
    private final HomekitServiceFactory serviceFactory;

    /** Factory for creating HomeKit characteristics */
    private final HomekitCharacteristicFactory characteristicFactory;

    /** Provider for HomeKit channel group types */
    private final HomekitChannelGroupTypeProvider channelGroupTypeProvider;

    public HomekitAccessoryThingHandler(Thing thing, HomekitAccessoryServerRegistry serverRegistry,
            HomekitAccessoryRegistry accessoryRegistry, HomekitChannelTypeProvider homekitChannelTypeProvider,
            HomekitChannelGroupTypeProvider channelGroupTypeProvider, HomekitThingTypeProvider homekitThingTypeProvider,
            HomekitEventManager eventManager, HomekitServiceFactory serviceFactory,
            HomekitCharacteristicFactory characteristicFactory) {
        super(thing, serverRegistry, accessoryRegistry, homekitChannelTypeProvider, homekitThingTypeProvider,
                eventManager);
        this.serviceFactory = serviceFactory;
        this.characteristicFactory = characteristicFactory;
        this.channelGroupTypeProvider = channelGroupTypeProvider;
    }

    /**
     * Initializes accessory-specific components.
     * 
     * This method initializes the channels for the accessory and sets up event subscriptions.
     */
    @Override
    protected void intializeSpecificComponents() {
        initializeChannels();
    }

    @SuppressWarnings("null")
    @Override
    protected void initializeChannels() {
        try {
            synchronized (characteristicMapLock) {
                characteristicMap.clear();
            }

            HomekitAccessory currentAccessory = getAccessory();
            if (currentAccessory == null) {
                throw new IllegalStateException("HomekitAccessory is not initialized");
            }

            // If the Thing does not have any channels, traverse the services of the accessory
            if (thing.getChannels().isEmpty()) {
                logger.info("{}Thing has no channels, traversing services of accessory", LOG_INIT);
                for (HomekitService service : currentAccessory.getServices()) {
                    addChannelGroupForService(service);
                }
            } else {
                // traverse channels and add them to the characteristicMap
                for (Channel channel : thing.getChannels()) {
                    if (channel.getUID().getGroupId() == null) {
                        logger.warn("{}Channel {} has no group ID", LOG_WARN, channel.getUID());
                        continue;
                    }

                    if (channel.getUID().getGroupId().split("\\.").length != 2) {
                        logger.warn("{}Channel {} has an invalid group ID", LOG_WARN, channel.getUID());
                        continue;
                    }

                    String serviceTag = channel.getUID().getGroupId().split("\\.")[0];
                    String serviceId = channel.getUID().getGroupId().split("\\.")[1];

                    String serviceType;
                    try {
                        serviceType = serviceFactory.getServiceTypeFromTag(serviceTag);
                    } catch (Exception e) {
                        logger.warn("{}HomekitService type could not be determined for service tag: {}", LOG_WARN,
                                serviceTag);
                        continue;
                    }

                    Optional<HomekitService> service = currentAccessory.getService(serviceType);
                    if (service.isEmpty()) {
                        logger.warn("{}HomekitService {} not found in accessory", LOG_WARN, serviceTag);
                        continue;
                    }

                    // verify that the serviceId matches the serviceId of the service
                    if (service.get().getInstanceId() != Long.parseLong(serviceId)) {
                        logger.warn("{}HomekitService ID {} does not match service ID {} for service {}", LOG_WARN,
                                serviceId, service.get().getInstanceId(), serviceTag);
                        continue;
                    }

                    String characteristicType;
                    try {
                        characteristicType = characteristicFactory
                                .getCharacteristicTypeFromTag(channel.getUID().getIdWithoutGroup());
                    } catch (Exception e) {
                        logger.warn("{}HomekitCharacteristic type could not be determined for characteristic tag: {}",
                                LOG_WARN, channel.getUID().getIdWithoutGroup());
                        continue;
                    }

                    Optional<HomekitCharacteristic<?>> characteristic = service.get()
                            .getCharacteristic(characteristicType);
                    if (characteristic.isEmpty()) {
                        logger.warn("{}HomekitCharacteristic {} not found in service {}", LOG_WARN, characteristicType,
                                serviceTag);
                        continue;
                    }
                    synchronized (characteristicMapLock) {
                        characteristicMap.put(channel, characteristic.get());
                    }
                }
            }
        } catch (HomekitException | IllegalStateException e) {
            handleError(ThingStatusDetail.CONFIGURATION_ERROR, "Failed to initialize channels: " + e.getMessage(), e);
            throw new IllegalStateException("Failed to initialize channels: " + e.getMessage(), e);
        }
    }

    /**
     * Adds a channel group for a HomeKit service.
     * 
     * ENHANCED WORKING EXAMPLE: Professional OpenHAB Channel Group Implementation
     * ===========================================================================
     * 
     * This method demonstrates the COMPLETE and CORRECT approach for OpenHAB channel groups:
     * 
     * 1. VALIDATION: Verifies that the channel group type exists via HomekitChannelGroupTypeProvider
     * 2. TYPE SAFETY: Uses proper ChannelGroupType definitions for better UI integration
     * 3. CHANNEL CREATION: Creates channels with group IDs for automatic grouping
     * 4. COMPREHENSIVE LOGGING: Provides detailed feedback for debugging and monitoring
     * 
     * The three-tier architecture:
     * - HomekitChannelGroupTypeProvider: Defines the group types and structure
     * - HomekitThingTypeProvider: References group types in thing definitions
     * - HomekitAccessoryThingHandler: Creates channels that reference those groups
     * 
     * This approach provides type safety, better UI integration, and follows OpenHAB best practices.
     *
     * @param service The HomeKit service to create a channel group for
     * @throws HomekitException if there is an error creating the channel group or if the service is null
     */
    protected void addChannelGroupForService(HomekitService service) throws HomekitException {
        if (service == null) {
            return;
        }

        try {
            String serviceTag = serviceFactory.getTagFromServiceType(service.getType());
            String groupId = serviceTag + "." + service.getInstanceId();

            // ENHANCED: Verify that the channel group type exists
            ChannelGroupTypeUID channelGroupTypeUID = new ChannelGroupTypeUID(HomekitBindingConstants.BINDING_ID,
                    "service-" + serviceTag);

            ChannelGroupType channelGroupType = channelGroupTypeProvider.getChannelGroupType(channelGroupTypeUID, null);

            if (channelGroupType == null) {
                logger.warn("{}No channel group type found for service: {} (UID: {}). Using basic approach.", LOG_WARN,
                        serviceTag, channelGroupTypeUID);
            } else {
                logger.debug("{}Found channel group type: '{}' with {} channel definitions", LOG_CHANNEL,
                        channelGroupType.getLabel(), channelGroupType.getChannelDefinitions().size());
            }

            logger.debug("{}Creating channel group for service '{}' with group ID: {} (type: {})", LOG_CHANNEL,
                    service.getName(), groupId, channelGroupType != null ? channelGroupType.getLabel() : "basic");

            // Create channels with group IDs in their UIDs - OpenHAB will group them automatically
            List<Channel> channels = new ArrayList<>();
            for (HomekitCharacteristic<?> characteristic : service.getCharacteristics()) {
                Channel channel = addChannelForCharacteristic(characteristic);
                if (channel != null) {
                    channels.add(channel);
                    logger.debug("{}Added channel '{}' to group '{}' (type: {})", LOG_CHANNEL, channel.getUID().getId(),
                            groupId, channelGroupType != null ? channelGroupType.getLabel() : "basic");
                }
            }

            if (channels.isEmpty()) {
                logger.warn("{}No channels created for service '{}'", LOG_WARN, service.getName());
                return;
            }

            // Update the thing with the new channels
            // The channels have group IDs in their UIDs, so OpenHAB groups them automatically
            ThingBuilder thingBuilder = editThing();
            for (Channel channel : channels) {
                thingBuilder.withChannel(channel);
            }
            updateThing(thingBuilder.build());

            logger.info("{}Successfully created channel group '{}' for service '{}' with {} channels (type: {})",
                    LOG_CHANNEL, groupId, service.getName(), channels.size(),
                    channelGroupType != null ? channelGroupType.getLabel() : "basic");

            // Log the channel structure for debugging
            for (Channel channel : channels) {
                logger.debug("{}  ├─ Channel: {} (Group: {}, Type: {})", LOG_CHANNEL, channel.getUID().getId(),
                        channel.getUID().getGroupId(), channel.getChannelTypeUID());
            }

            if (channelGroupType != null) {
                logger.debug("{}  └─ Group Type: {} ({})", LOG_CHANNEL, channelGroupType.getLabel(),
                        channelGroupType.getDescription());
            }

        } catch (Exception e) {
            logger.error("{}Failed to add channel group for service '{}': {}", LOG_WARN, service.getInstanceId(),
                    e.getMessage(), e);
            throw new HomekitException("Failed to add channel group for service " + service.getInstanceId(), e);
        }
    }

    /**
     * Adds a channel for a HomeKit characteristic.
     * 
     * This method creates a channel for the specified HomeKit characteristic, using the characteristic's
     * type and description to configure the channel. The channel is then added to the characteristic map
     * for state tracking.
     *
     * @param characteristic The HomeKit characteristic to create a channel for
     * @return The created channel, or null if creation fails
     * @throws HomekitException if there is an error creating the channel or if the characteristic type is not found
     */
    @Override
    protected Channel addChannelForCharacteristic(HomekitCharacteristic<?> characteristic) throws HomekitException {
        try {
            // Let subclasses determine the channel ID
            ChannelUID channelUID = getChannelUID(characteristic);

            ChannelTypeUID channelTypeUID = new ChannelTypeUID(HomekitBindingConstants.BINDING_ID,
                    characteristic.getType());
            ChannelType channelType = homekitChannelTypeProvider.getChannelType(channelTypeUID, null);

            if (channelType == null) {
                logger.warn("{}No ChannelType found for characteristic {}", LOG_CHANNEL, characteristic.getUID());
                throw new HomekitException("No ChannelType found for characteristic " + characteristic.getUID());
            }

            Channel channel = ChannelBuilder.create(channelUID).withType(channelTypeUID)
                    .withLabel(characteristic.getDescription()).withDescription(characteristic.getDescription())
                    .build();

            synchronized (characteristicMapLock) {
                characteristicMap.put(channel, characteristic);
            }

            logger.debug("{}Added channel {} for characteristic {}", LOG_CHANNEL, channelUID, characteristic.getUID());
            return channel;
        } catch (HomekitException e) {
            logger.warn("{}Unexpected error adding channel for characteristic {}: {}", LOG_CHANNEL,
                    characteristic.getUID(), e.getMessage());
            throw new HomekitException("Unexpected error adding channel for characteristic " + characteristic.getUID(),
                    e);
        }
    }

    /**
     * Handles accessory-specific disposal operations.
     * 
     * This method cleans up any resources specific to the accessory handler.
     */
    @Override
    protected void handleSpecificDispose() {
        // No additional cleanup needed for accessory handler
    }

    /**
     * Validates accessory-specific configuration.
     * 
     * This method checks that the configuration contains all required parameters
     * for the accessory handler.
     *
     * @param config The configuration to validate
     * @throws IllegalArgumentException if the configuration is invalid
     */
    @Override
    protected void validateSpecificConfiguration(Configuration config) {
        // No additional configuration validation needed for accessory handler
    }

    /**
     * Determines the thing status based on the current state.
     * 
     * This method evaluates the current state of the accessory and returns
     * the appropriate thing status.
     *
     * @return The current thing status
     */
    @Override
    protected ThingStatus determineThingStatus() {
        if (!serverConnected || !serverPaired || !accessoryAvailable) {
            return ThingStatus.OFFLINE;
        }
        return ThingStatus.ONLINE;
    }

    /**
     * Determines the thing status detail based on the current state.
     * 
     * This method evaluates the current state of the accessory and returns
     * the appropriate status detail.
     *
     * @return The current thing status detail
     */
    @Override
    protected ThingStatusDetail determineThingStatusDetail() {
        if (!serverConnected) {
            return ThingStatusDetail.COMMUNICATION_ERROR;
        }
        if (!serverPaired) {
            return ThingStatusDetail.CONFIGURATION_ERROR;
        }
        if (!accessoryAvailable) {
            return ThingStatusDetail.COMMUNICATION_ERROR;
        }
        return ThingStatusDetail.NONE;
    }

    /**
     * Determines the thing status description based on the current state.
     * 
     * This method evaluates the current state of the accessory and returns
     * a description of the current status.
     *
     * @return A description of the current thing status
     */
    @Override
    protected String determineThingStatusDescription() {
        if (!serverConnected) {
            return "Server disconnected";
        }
        if (!serverPaired) {
            return "Server not paired";
        }
        if (!accessoryAvailable) {
            return "HomekitAccessory not available";
        }
        return "Server connected and paired";
    }

    // ========== Channel Management Methods ==========
    /**
     * Gets the current characteristics of the accessory.
     * 
     * This method retrieves all characteristics associated with the accessory.
     *
     * @return A set of characteristics for the accessory
     */
    @Override
    protected Set<HomekitCharacteristic<?>> getCurrentCharacteristics() {
        HomekitAccessory currentAccessory = getAccessory();
        if (currentAccessory != null) {
            Set<HomekitCharacteristic<?>> characteristics = new HashSet<>();
            for (HomekitService service : currentAccessory.getServices()) {
                for (HomekitCharacteristic<?> characteristic : service.getCharacteristics()) {
                    characteristics.add(characteristic);
                }
            }
            return characteristics;
        }
        return Collections.emptySet();
    }

    /**
     * Gets the channel UID for a characteristic.
     * 
     * WORKING EXAMPLE: This method demonstrates the CORRECT OpenHAB channel group pattern!
     * ====================================================================================
     * 
     * This method constructs a channel UID that includes a group ID, which enables OpenHAB's
     * automatic channel grouping functionality. Here's how it works:
     * 
     * 1. GROUP ID CONSTRUCTION: Creates "serviceTag.instanceId" (e.g., "lightbulb.1")
     * 2. CHANNEL UID FORMAT: "binding:thingType:thingId:groupId#channelId"
     * 3. AUTOMATIC GROUPING: OpenHAB sees the group ID and groups channels automatically
     * 
     * Example output: "homekit:accessory:myDevice:lightbulb.1#brightness"
     * │ │ │
     * │ │ └── Channel within group
     * │ └─────────── Group ID
     * └──────────────────────────────────── Thing identifier
     * 
     * This is the KEY to making channel groups work in OpenHAB!
     *
     * @param characteristic The characteristic to get the channel UID for
     * @return The channel UID for the characteristic (with group ID embedded)
     * @throws IllegalArgumentException if the service or characteristic type cannot be determined
     */
    @Override
    protected ChannelUID getChannelUID(HomekitCharacteristic<?> characteristic) {
        String groupId;
        try {
            // Create group ID: "serviceTag.instanceId" (e.g., "lightbulb.1", "fan.2", etc.)
            groupId = serviceFactory.getTagFromServiceType(characteristic.getService().getType()) + "."
                    + characteristic.getService().getInstanceId();
        } catch (Exception e) {
            throw new IllegalArgumentException("HomekitService tag could not be determined", e);
        }
        String characteristicTag;
        try {
            characteristicTag = characteristicFactory.getTagFromCharacteristicType(characteristic.getType());
        } catch (Exception e) {
            throw new IllegalArgumentException("HomekitCharacteristic tag could not be determined", e);
        }

        // This creates the magic: ChannelUID with group ID that enables automatic grouping
        // Format: "binding:thingType:thingId:groupId#channelId"
        return new ChannelUID(thing.getUID(), groupId, characteristicTag);
    }

    /**
     * Validates that a characteristic belongs to this handler.
     * 
     * This method checks that the characteristic is associated with the handler's accessory.
     *
     * @param characteristic The characteristic to validate
     * @return true if the characteristic belongs to this handler, false otherwise
     */
    @Override
    protected boolean validateCharacteristicBelongsToHandler(HomekitCharacteristic<?> characteristic) {
        HomekitAccessory currentAccessory = getAccessory();
        if (currentAccessory != null) {
            for (HomekitService service : currentAccessory.getServices()) {
                if (service.getCharacteristics().contains(characteristic)) {
                    return true;
                }
            }
        }
        return false;
    }

    // ========== Recovery Methods ==========
    /**
     * Performs accessory-specific recovery operations.
     * 
     * This method attempts to recover the accessory's state after an error.
     */
    @Override
    protected void performSpecificRecovery() {
        // no additional recovery steps needed for accessory handler
    }
}
