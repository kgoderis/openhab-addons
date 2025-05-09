package org.openhab.io.homekit.handler;

import java.util.Collections;
import java.util.HashSet;
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
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristic;
import org.openhab.io.homekit.api.registry.HomekitAccessoryRegistry;
import org.openhab.io.homekit.api.registry.HomekitAccessoryServerRegistry;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.exception.HomekitException;
import org.openhab.io.homekit.network.discovery.HomekitBindingConstants;
import org.openhab.io.homekit.provider.HomekitChannelTypeProvider;
import org.openhab.io.homekit.provider.HomekitThingTypeProvider;

@NonNullByDefault
public class HomekitAccessoryThingHandler extends AbstractHomekitHandler {
    // HomekitAccessory-specific fields

    public HomekitAccessoryThingHandler(Thing thing, HomekitAccessoryServerRegistry serverRegistry,
            HomekitAccessoryRegistry accessoryRegistry, HomekitChannelTypeProvider homekitChannelTypeProvider,
            HomekitThingTypeProvider homekitThingTypeProvider, HomekitEventManager eventManager) {
        super(thing, serverRegistry, accessoryRegistry, homekitChannelTypeProvider, homekitThingTypeProvider,
                eventManager);
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

            // If the Thing does not have any channels, traverse the services of the accessory, and for each service add
            // a
            // ChannelGroup. then, for each characteristic of the service, add a channel in the ChannelGroup. Add
            // logging to
            // the process.
            if (thing.getChannels().isEmpty()) {
                logger.info("Thing has no channels, traversing services of accessory");
                for (HomekitService service : currentAccessory.getServices()) {
                    // addChannelGroupForService(service);
                    for (HomekitCharacteristic<?> characteristic : service.getCharacteristics()) {
                        addChannelForCharacteristic(characteristic);
                    }
                }
            } else {
                // traverse channels and add them to the characteristicMap
                for (Channel channel : thing.getChannels()) {

                    if (channel.getUID().getGroupId() == null) {
                        logger.warn("Channel {} has no group ID", channel.getUID());
                        continue;
                    }

                    if (channel.getUID().getGroupId().split("\\.").length != 2) {
                        logger.warn("Channel {} has an invalid group ID", channel.getUID());
                        continue;
                    }

                    String serviceTag = channel.getUID().getGroupId().split("\\.")[0];
                    String serviceId = channel.getUID().getGroupId().split("\\.")[1];

                    String serviceType;
                    try {
                        serviceType = homekitThingTypeProvider.getServiceTypeFromTag(serviceTag);
                    } catch (HomekitException e) {
                        logger.warn("HomekitService type could not be determined for service tag: {}", serviceTag);
                        continue;
                    }

                    Optional<HomekitService> service = currentAccessory.getService(serviceType);
                    if (service.isEmpty()) {
                        logger.warn("HomekitService {} not found in accessory", serviceTag);
                        continue;
                    }

                    // verify that the serviceId matches the serviceId of the service
                    if (service.get().getInstanceId() != Long.parseLong(serviceId)) {
                        logger.warn("HomekitService ID {} does not match service ID {} for service {}", serviceId,
                                service.get().getInstanceId(), serviceTag);
                        continue;
                    }

                    String characteristicType;
                    try {
                        characteristicType = homekitChannelTypeProvider
                                .getCharacteristicTypeFromTag(channel.getUID().getIdWithoutGroup());
                    } catch (HomekitException e) {
                        logger.warn("HomekitCharacteristic type could not be determined for characteristic tag: {}",
                                channel.getUID().getIdWithoutGroup());
                        continue;
                    }

                    Optional<HomekitCharacteristic<?>> characteristic = service.get().getCharacteristic(characteristicType);
                    if (characteristic.isEmpty()) {
                        logger.warn("HomekitCharacteristic {} not found in service {}", characteristicType, serviceTag);
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

    @Override
    protected Channel addChannelForCharacteristic(HomekitCharacteristic<?> characteristic) throws HomekitException {
        try {
            // Let subclasses determine the channel ID
            ChannelUID channelUID = getChannelUID(characteristic);

            ChannelTypeUID channelTypeUID = new ChannelTypeUID(HomekitBindingConstants.BINDING_ID,
                    characteristic.getInstanceType());
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

            updateThing(editThing().withChannel(channel).build());
            return channel;
        } catch (HomekitException e) {
            logger.warn("{}Unexpected error adding channel for characteristic {}: {}", LOG_CHANNEL,
                    characteristic.getUID(), e.getMessage());
            throw new HomekitException("Unexpected error adding channel for characteristic " + characteristic.getUID(),
                    e);
        }
    }

    // protected void addChannelGroupForService(HomekitService service) {
    // // Implementation needed for channel group creation
    // if (service == null) {
    // return;
    // }

    // try {
    // String serviceTag = homekitThingTypeProvider.getServiceTag(service.getInstanceType());
    // String groupId = serviceTag + "_" + service.getInstanceId();

    // // // Create channel group for service
    // // ChannelGroupBuilder groupBuilder = ChannelGroupBuilder
    // // .create(new ChannelGroupUID(thing.getUID(), groupId), service.getDescription())
    // // .withLabel(service.getDescription());

    // // Add channel group to thing
    // ThingBuilder thingBuilder = editThing();
    // thingBuilder.withChannelGroup(groupBuilder.build());
    // updateThing(thingBuilder.build());

    // logger.debug("{}Added channel group for service {} with ID {}", LOG_PREFIX, service.getDescription(),
    // groupId);
    // } catch (Exception e) {
    // logger.warn("{}Warning - Type: Channel Group, HomekitMessage: Failed to add channel group for service {}: {}",
    // LOG_PREFIX, service.getInstanceId(), e.getMessage());
    // }
    // }

    // private void addChannelGroupForService(HomekitService service) {
    // logger.info("Adding channel group for service: {}", service.getUID());
    // try {
    // String groupId = homekitThingTypeProvider.getServiceTag(service.getInstanceType()) + "."
    // + service.getInstanceId();
    // ChannelGroupTypeUID channelGroupTypeUID = homekitChannelGroupTypeProvider
    // .getChannelGroupTypeUID(service.getInstanceType());

    // // Create a list to hold all channels for this group
    // List<Channel> channels = new ArrayList<>();

    // // Add channels for each characteristic
    // for (HomekitCharacteristic<?> characteristic : service.getCharacteristics()) {
    // ChannelUID channelUID = new ChannelUID(thing.getUID(), groupId,
    // homekitChannelTypeProvider.getCharacteristicTag(characteristic.getInstanceType()));
    // ChannelTypeUID channelTypeUID = new ChannelTypeUID(HomekitBindingConstants.BINDING_ID,
    // characteristic.getInstanceType());

    // ChannelType channelType = homekitChannelTypeProvider.getChannelType(channelTypeUID, null);
    // if (channelType == null) {
    // logger.warn("No ChannelType found for characteristic {}", characteristic.getUID());
    // continue;
    // }

    // Channel channel = ChannelBuilder.create(channelUID).withType(channelTypeUID)
    // .withLabel(characteristic.getDescription()).build();

    // channels.add(channel);
    // characteristicMap.put(channel, characteristic);
    // }

    // // Create a ThingBuilder to modify the thing
    // ThingBuilder thingBuilder = editThing();

    // // Add all channels at once
    // channels.forEach(thingBuilder::withChannel);

    // // Update the thing with all new channels
    // updateThing(thingBuilder.build());

    // } catch (IllegalArgumentException | HomekitException e) {
    // logger.warn("Error adding channel group for service {}: {}", service.getUID(), e.getMessage());
    // }
    // }

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
            groupId = homekitThingTypeProvider.getServiceTag(characteristic.getService().getInstanceType()) + "."
                    + characteristic.getService().getInstanceId();

        } catch (HomekitException e) {
            throw new IllegalArgumentException("HomekitService tag could not be determined", e);
        }
        String characteristicTag;
        try {
            characteristicTag = homekitChannelTypeProvider.getCharacteristicTag(characteristic.getInstanceType());
        } catch (HomekitException e) {
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
