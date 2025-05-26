package org.openhab.io.homekit.handler;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelGroupUID;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.binding.builder.ThingBuilder;
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
import org.openhab.io.homekit.provider.HomekitChannelTypeProvider;
import org.openhab.io.homekit.provider.HomekitThingTypeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
public class HomekitAccessoryThingHandler extends AbstractHomekitHandler {
    // ========== Log Message Prefixes ==========
    private static final String LOG_PREFIX = "Homekit AccessoryThingHandler: ";
    private static final String LOG_INIT = LOG_PREFIX + "Init - ";
    private static final String LOG_STATE = LOG_PREFIX + "State - ";
    private static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    private static final String LOG_CHANNEL = LOG_PREFIX + "Channel - ";
    private static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    private static final String LOG_WARN = LOG_PREFIX + "Warning - ";

    private final Logger logger = LoggerFactory.getLogger(HomekitAccessoryThingHandler.class);

    // HomekitAccessory-specific fields
    private final HomekitServiceFactory serviceFactory;
    private final HomekitCharacteristicFactory characteristicFactory;

    public HomekitAccessoryThingHandler(Thing thing, HomekitAccessoryServerRegistry serverRegistry,
            HomekitAccessoryRegistry accessoryRegistry, HomekitChannelTypeProvider homekitChannelTypeProvider,
            HomekitThingTypeProvider homekitThingTypeProvider, HomekitEventManager eventManager,
            HomekitServiceFactory serviceFactory, HomekitCharacteristicFactory characteristicFactory) {
        super(thing, serverRegistry, accessoryRegistry, homekitChannelTypeProvider, homekitThingTypeProvider,
                eventManager);
        this.serviceFactory = serviceFactory;
        this.characteristicFactory = characteristicFactory;
    }

    // ========== Core Lifecycle Methods ==========
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
     * @param service The HomeKit service to create a channel group for
     * @throws HomekitException if there is an error creating the channel group
     */
    protected void addChannelGroupForService(HomekitService service) throws HomekitException {
        if (service == null) {
            return;
        }

        try {
            String serviceTag = serviceFactory.getTagFromServiceType(service.getType());
            String groupId = serviceTag + "." + service.getInstanceId();
            ChannelGroupUID channelGroupUID = new ChannelGroupUID(thing.getUID(), groupId);

            // Create channel group for service
            ThingBuilder thingBuilder = editThing();

            // Add channels for each characteristic
            for (HomekitCharacteristic<?> characteristic : service.getCharacteristics()) {
                Channel channel = addChannelForCharacteristic(characteristic);
                if (channel != null) {
                    thingBuilder.withChannel(channel);
                }
            }

            // Update the thing with the new channels
            updateThing(thingBuilder.build());

            logger.debug("{}Added channel group for service {} with ID {} (channels only, group not created at runtime)", LOG_CHANNEL, service.getName(), groupId);
        } catch (Exception e) {
            logger.warn("{}Failed to add channel group for service {}: {}", LOG_WARN, service.getInstanceId(),
                    e.getMessage());
            throw new HomekitException("Failed to add channel group for service " + service.getInstanceId(), e);
        }
    }

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

    @Override
    protected void handleSpecificDispose() {
        // No additional cleanup needed for accessory handler
    }

    // ========== Configuration Methods ==========
    @Override
    protected void validateSpecificConfiguration(Configuration config) {
        // No additional configuration validation needed for accessory handler
    }

    // ========== State Management Methods ==========
    @Override
    protected ThingStatus determineThingStatus() {
        if (!serverConnected || !serverPaired || !accessoryAvailable) {
            return ThingStatus.OFFLINE;
        }
        return ThingStatus.ONLINE;
    }

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

    @Override
    protected ChannelUID getChannelUID(HomekitCharacteristic<?> characteristic) {
        String groupId;
        try {
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
        return new ChannelUID(thing.getUID(), groupId, characteristicTag);
    }

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
    @Override
    protected void performSpecificRecovery() throws Exception {
        // no additional recovery steps needed for accessory handler
    }
}
