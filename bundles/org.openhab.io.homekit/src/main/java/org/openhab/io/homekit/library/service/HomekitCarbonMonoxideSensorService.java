package org.openhab.io.homekit.library.service;

import javax.json.JsonValue;

import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.service.HomekitServiceType;
import org.openhab.io.homekit.core.service.AbstractHomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.library.characteristic.HomekitCarbonMonoxideDetectedCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitStatusActiveCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitStatusFaultCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitStatusLowBatteryCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitStatusTamperedCharacteristic;

/**
 * Service that represents a carbon monoxide sensor in HomeKit.
 * This service provides control over carbon monoxide sensors and similar devices.
 */
@HomekitServiceType(type = "0000007F-0000-1000-8000-0026BB765291", name = "CarbonMonoxideSensor", tag = "CarbonMonoxideSensor")
public class HomekitCarbonMonoxideSensorService extends AbstractHomekitService {

    /**
     * Creates a new HomekitCarbonMonoxideSensorService.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     */
    public HomekitCarbonMonoxideSensorService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory) {
        super(accessory, eventManager, characteristicFactory);
    }

    /**
     * Creates a new HomekitCarbonMonoxideSensorService from a JSON value.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @param value JSON value containing service configuration
     */
    public HomekitCarbonMonoxideSensorService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory, JsonValue value) {
        super(accessory, eventManager, characteristicFactory, value);
    }

    @Override
    public void addCharacteristics() {
        super.addCharacteristics();
        addCharacteristic(new HomekitCarbonMonoxideDetectedCharacteristic(this,
                getAccessory().getNextAvailableInstanceId(), eventManager));
        addCharacteristic(
                new HomekitStatusActiveCharacteristic(this, getAccessory().getNextAvailableInstanceId(), eventManager));
        addCharacteristic(
                new HomekitStatusFaultCharacteristic(this, getAccessory().getNextAvailableInstanceId(), eventManager));
        addCharacteristic(new HomekitStatusTamperedCharacteristic(this, getAccessory().getNextAvailableInstanceId(),
                eventManager));
        addCharacteristic(new HomekitStatusLowBatteryCharacteristic(this, getAccessory().getNextAvailableInstanceId(),
                eventManager));
    }

    @Override
    public boolean isExtensible() {
        return false;
    }
}
