package org.openhab.io.homekit.internal.handler;

import java.util.Collections;
import java.util.HashSet;
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
import org.openhab.io.homekit.api.hap.Accessory;
import org.openhab.io.homekit.api.hap.Characteristic;
import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.api.registry.AccessoryRegistry;
import org.openhab.io.homekit.api.registry.AccessoryServerRegistry;
import org.openhab.io.homekit.exception.HomekitException;
import org.openhab.io.homekit.internal.client.HomekitBindingConstants;
import org.openhab.io.homekit.internal.provider.HomekitChannelTypeProvider;
import org.openhab.io.homekit.internal.provider.HomekitThingTypeProvider;

@NonNullByDefault
public class AccessoryThingHandler extends AbstractHomekitHandler {
    // Accessory-specific fields

    public AccessoryThingHandler(Thing thing, AccessoryServerRegistry serverRegistry,
            AccessoryRegistry accessoryRegistry, HomekitChannelTypeProvider homekitChannelTypeProvider,
            HomekitThingTypeProvider homekitThingTypeProvider) {
        super(thing, serverRegistry, accessoryRegistry, homekitChannelTypeProvider, homekitThingTypeProvider);
    }

    // ========== Core Lifecycle Methods ==========
    @Override
    protected void handleSpecificInitialization() {
        initializeChannels();
    }

    @SuppressWarnings("null")
    @Override
    protected void initializeChannels() {
        try {
            synchronized (characteristicMapLock) {
                characteristicMap.clear();
            }

            Accessory currentAccessory = getAccessory();
            if (currentAccessory == null) {
                throw new IllegalStateException("Accessory is not initialized");
            }

            // If the Thing does not have any channels, traverse the services of the accessory, and for each service add
            // a
            // ChannelGroup. then, for each characteristic of the service, add a channel in the ChannelGroup. Add
            // logging to
            // the process.
            if (thing.getChannels().isEmpty()) {
                logger.info("Thing has no channels, traversing services of accessory");
                for (Service service : currentAccessory.getServices()) {
                    // addChannelGroupForService(service);
                    for (Characteristic<?> characteristic : service.getCharacteristics()) {
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
                        logger.warn("Service type could not be determined for service tag: {}", serviceTag);
                        continue;
                    }

                    Service service = currentAccessory.getService(serviceType);
                    if (service == null) {
                        logger.warn("Service {} not found in accessory", serviceTag);
                        continue;
                    }

                    // verify that the serviceId matches the serviceId of the service
                    if (service.getInstanceId() != Long.parseLong(serviceId)) {
                        logger.warn("Service ID {} does not match service ID {} for service {}", serviceId,
                                service.getInstanceId(), serviceTag);
                        continue;
                    }

                    String characteristicType;
                    try {
                        characteristicType = homekitChannelTypeProvider
                                .getCharacteristicTypeFromTag(channel.getUID().getIdWithoutGroup());
                    } catch (HomekitException e) {
                        logger.warn("Characteristic type could not be determined for characteristic tag: {}",
                                channel.getUID().getIdWithoutGroup());
                        continue;
                    }

                    Characteristic<?> characteristic = service.getCharacteristic(characteristicType);
                    if (characteristic == null) {
                        logger.warn("Characteristic {} not found in service {}", characteristicType, serviceTag);
                        continue;
                    }
                    synchronized (characteristicMapLock) {
                        characteristicMap.put(channel, characteristic);
                    }
                }
            }
        } catch (Exception e) {
            handleError(ThingStatusDetail.CONFIGURATION_ERROR, "Failed to initialize channels: " + e.getMessage(), e);
            throw new IllegalStateException("Failed to initialize channels: " + e.getMessage(), e);
        }
    }

    @Override
    protected Channel addChannelForCharacteristic(Characteristic<?> characteristic) throws HomekitException {
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
        } catch (Exception e) {
            logger.warn("{}Unexpected error adding channel for characteristic {}: {}", LOG_CHANNEL,
                    characteristic.getUID(), e.getMessage());
            throw new HomekitException("Unexpected error adding channel for characteristic " + characteristic.getUID(),
                    e);
        }
    }

    // protected void addChannelGroupForService(Service service) {
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
    // logger.warn("{}Warning - Type: Channel Group, Message: Failed to add channel group for service {}: {}",
    // LOG_PREFIX, service.getInstanceId(), e.getMessage());
    // }
    // }

    // private void addChannelGroupForService(Service service) {
    // logger.info("Adding channel group for service: {}", service.getUID());
    // try {
    // String groupId = homekitThingTypeProvider.getServiceTag(service.getInstanceType()) + "."
    // + service.getInstanceId();
    // ChannelGroupTypeUID channelGroupTypeUID = homekitChannelGroupTypeProvider
    // .getChannelGroupTypeUID(service.getInstanceType());

    // // Create a list to hold all channels for this group
    // List<Channel> channels = new ArrayList<>();

    // // Add channels for each characteristic
    // for (Characteristic<?> characteristic : service.getCharacteristics()) {
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
            return "Accessory not available";
        }
        return "Server connected and paired";
    }

    // ========== Channel Management Methods ==========
    @Override
    protected Set<Characteristic<?>> getCurrentCharacteristics() {
        Accessory currentAccessory = getAccessory();
        if (currentAccessory != null) {
            Set<Characteristic<?>> characteristics = new HashSet<>();
            for (Service service : currentAccessory.getServices()) {
                for (Characteristic<?> characteristic : service.getCharacteristics()) {
                    characteristics.add(characteristic);
                }
            }
            return characteristics;
        }
        return Collections.emptySet();
    }

    @Override
    protected ChannelUID getChannelUID(Characteristic<?> characteristic) {
        String groupId;
        try {
            groupId = homekitThingTypeProvider.getServiceTag(characteristic.getService().getInstanceType()) + "."
                    + characteristic.getService().getInstanceId();

        } catch (HomekitException e) {
            throw new IllegalArgumentException("Service tag could not be determined", e);
        }
        String characteristicTag;
        try {
            characteristicTag = homekitChannelTypeProvider.getCharacteristicTag(characteristic.getInstanceType());
        } catch (HomekitException e) {
            throw new IllegalArgumentException("Characteristic tag could not be determined", e);
        }
        return new ChannelUID(thing.getUID(), groupId, characteristicTag);
    }

    @Override
    protected boolean validateCharacteristicBelongsToHandler(Characteristic<?> characteristic) {
        Accessory currentAccessory = getAccessory();
        if (currentAccessory != null) {
            for (Service service : currentAccessory.getServices()) {
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
