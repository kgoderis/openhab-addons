package org.openhab.io.homekit.library.service;

import javax.json.JsonValue;

import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.service.HomekitServiceType;
import org.openhab.io.homekit.core.service.AbstractHomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.library.characteristic.HomekitBatteryLevelCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitChargingStateCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitStatusLowBatteryCharacteristic;

/**
 * Service that represents a battery in HomeKit.
 * This service provides control over battery-powered devices.
 */
@HomekitServiceType(type = "00000096-0000-1000-8000-0026BB765291", name = "Battery", tag = "Battery")
public class HomekitBatteryService extends AbstractHomekitService {

    /**
     * Creates a new HomekitBatteryService.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     */
    public HomekitBatteryService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory) {
        super(accessory, eventManager, characteristicFactory);
    }

    /**
     * Creates a new HomekitBatteryService from a JSON value.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @param value JSON value containing service configuration
     */
    public HomekitBatteryService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory, JsonValue value) {
        super(accessory, eventManager, characteristicFactory, value);
    }

    @Override
    public void addCharacteristics() {
        super.addCharacteristics();
        addCharacteristic(
                new HomekitBatteryLevelCharacteristic(this, getAccessory().getNextAvailableInstanceId(), eventManager));
        addCharacteristic(new HomekitChargingStateCharacteristic(this, getAccessory().getNextAvailableInstanceId(),
                eventManager));
        addCharacteristic(new HomekitStatusLowBatteryCharacteristic(this, getAccessory().getNextAvailableInstanceId(),
                eventManager));
    }

    @Override
    public boolean isExtensible() {
        return false;
    }
}
