package org.openhab.io.homekit.library.service;

import javax.json.JsonValue;

import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.service.HomekitServiceType;
import org.openhab.io.homekit.core.service.AbstractHomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.library.characteristic.HomekitCurrentHeatingCoolingStateCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitCurrentTemperatureCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitTargetHeatingCoolingStateCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitTargetTemperatureCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitTemperatureDisplayUnitsCharacteristic;

/**
 * Service that represents a thermostat in HomeKit.
 * This service provides control over heating and cooling systems.
 */
@HomekitServiceType(type = "0000004A-0000-1000-8000-0026BB765291", name = "Thermostat", tag = "Thermostat")
public class HomekitThermostatService extends AbstractHomekitService {

    /**
     * Creates a new HomekitThermostatService.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     */
    public HomekitThermostatService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory) {
        super(accessory, eventManager, characteristicFactory);
    }

    /**
     * Creates a new HomekitThermostatService from a JSON value.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @param value JSON value containing service configuration
     */
    public HomekitThermostatService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory, JsonValue value) {
        super(accessory, eventManager, characteristicFactory, value);
    }

    @Override
    public void addCharacteristics() {
        super.addCharacteristics();
        addCharacteristic(new HomekitCurrentHeatingCoolingStateCharacteristic(this,
                getAccessory().getNextAvailableInstanceId(), eventManager));
        addCharacteristic(new HomekitTargetHeatingCoolingStateCharacteristic(this,
                getAccessory().getNextAvailableInstanceId(), eventManager));
        addCharacteristic(new HomekitCurrentTemperatureCharacteristic(this, getAccessory().getNextAvailableInstanceId(),
                eventManager));
        addCharacteristic(new HomekitTargetTemperatureCharacteristic(this, getAccessory().getNextAvailableInstanceId(),
                eventManager));
        addCharacteristic(new HomekitTemperatureDisplayUnitsCharacteristic(this,
                getAccessory().getNextAvailableInstanceId(), eventManager));
    }

    @Override
    public boolean isExtensible() {
        return false;
    }
}
