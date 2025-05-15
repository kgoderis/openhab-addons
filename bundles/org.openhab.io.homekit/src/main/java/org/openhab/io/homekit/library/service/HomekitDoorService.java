package org.openhab.io.homekit.library.service;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.service.HomekitServiceType;
import org.openhab.io.homekit.core.service.AbstractHomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.library.characteristic.HomekitCurrentPositionCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitHoldPositionCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitNameCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitObstructionDetectedCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitPositionStateCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitTargetPositionCharacteristic;

/**
 * Service that represents a door in HomeKit.
 * This service provides control over door position and state.
 *
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitServiceType(type = "00000081-0000-1000-8000-0026BB765291", name = "Door", tag = "door")
@NonNullByDefault
public class HomekitDoorService extends AbstractHomekitService {

    /**
     * Creates a new HomekitDoorService.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     */
    public HomekitDoorService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory) {
        super(accessory, eventManager, characteristicFactory);
        withName("Door").withPrimary(false).withHidden(false);
    }

    /**
     * Creates a new HomekitDoorService from a JSON value.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @param value JSON value containing service configuration
     */
    public HomekitDoorService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory, JsonValue value) {
        super(accessory, eventManager, characteristicFactory, value);
    }

    @Override
    public void addCharacteristics() {
        addCharacteristic(new HomekitCurrentPositionCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        addCharacteristic(
                new HomekitTargetPositionCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(true));
        addCharacteristic(
                new HomekitPositionStateCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(true));
        addCharacteristic(
                new HomekitHoldPositionCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(false));
        addCharacteristic(new HomekitObstructionDetectedCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(false));
        addCharacteristic(new HomekitNameCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                .withMandatory(false));
    }

    @Override
    public boolean isExtensible() {
        return false;
    }
}
