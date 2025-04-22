package org.openhab.io.homekit.internal.handler;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.io.homekit.api.hap.Accessory;
import org.openhab.io.homekit.api.hap.Characteristic;
import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.api.registry.AccessoryRegistry;
import org.openhab.io.homekit.api.registry.AccessoryServerRegistry;
import org.openhab.io.homekit.internal.events.AccessoryEvent;
import org.openhab.io.homekit.internal.provider.HomekitChannelGroupTypeProvider;
import org.openhab.io.homekit.internal.provider.HomekitChannelTypeProvider;
import org.openhab.io.homekit.internal.provider.HomekitThingTypeProvider;

@NonNullByDefault
public class AccessoryThingHandler extends AbstractHomekitHandler {
    // Accessory-specific fields
    private volatile boolean accessoryAvailable = false;
    private final HomekitChannelGroupTypeProvider homekitChannelGroupTypeProvider;

    public AccessoryThingHandler(Thing thing, AccessoryServerRegistry serverRegistry,
            AccessoryRegistry accessoryRegistry, HomekitChannelTypeProvider homekitChannelTypeProvider,
            HomekitThingTypeProvider homekitThingTypeProvider,
            HomekitChannelGroupTypeProvider homekitChannelGroupTypeProvider) {
        super(thing, serverRegistry, accessoryRegistry, homekitChannelTypeProvider, homekitThingTypeProvider);
        this.homekitChannelGroupTypeProvider = homekitChannelGroupTypeProvider;
    }

    // ========== Core Lifecycle Methods ==========
    @Override
    protected void handleSpecificInitialization() {
        initializeChannels();
    }


    private void initializeChannels() {
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
                for (Service service : accessory.getServices()) {
                    addChannelGroupForService(service);
                }
            } else {
                // traverse channels and add them to the characteristicMap
                for (Channel channel : thing.getChannels()) {

                    String serviceTag = channel.getUID().getGroupId().split("\\.")[0];
                    String serviceId = channel.getUID().getGroupId().split("\\.")[1];

                    String serviceType;
                    try {
                        serviceType = homekitThingTypeProvider.getServiceTypeFromTag(serviceTag);
                    } catch (HomekitException e) {
                        logger.warn("Service type could not be determined for service tag: {}", serviceTag);
                        continue;
                    }

                    Service service = accessory.getService(serviceType);
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
        return null;
    }

    // ========== Event Handling Methods ==========
    @Override
    protected void handleAccessoryServiceAdded(AccessoryEvent event) {
        synchronized (accessoryLock) {
            if (accessory == null) {
                Accessory foundAccessory = accessoryRegistry.get(new AccessoryUID(accessoryId));
                if (foundAccessory != null) {
                    setAccessory(foundAccessory);
                    foundAccessory.addChangeListener(this);
                    validateAndUpdateState();
                } else {
                    logger.warn("{}Accessory not found in registry after add event", LOG_EVENT);
                    updateState(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Accessory not found");
                }
            }
        }
    }

    @Override
    protected void handleAccessoryServiceRemoved(AccessoryEvent event) {
        synchronized (accessoryLock) {
            if (accessory != null) {
                try {
                    accessory.removeChangeListener(this);
                } catch (Exception e) {
                    logger.warn("{}Failed to remove accessory change listener: {}", LOG_EVENT, e.getMessage());
                }
                accessory = null;
                updateState(ThingStatus.OFFLINE, ThingStatusDetail.GONE, "Accessory removed");
            }
        }
        handleServiceRemoved();
    }

    @Override
    protected void handleAccessoryServiceStateChanged(AccessoryEvent event) {
        synchronized (accessoryLock) {
            if (accessory != null) {
                Service foundService = accessory.getService(serviceId);
                if (foundService != null) {
                    setService(foundService);
                    validateAndUpdateState();
                    synchronizeChannels();
                } else {
                    logger.warn("{}Service not found in accessory after change event", LOG_EVENT);
                    updateState(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Service not found");
                }
            }
        }
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
    protected String getChannelId(Characteristic<?> characteristic) {
        return characteristic.getUID().getHomekitId();
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
