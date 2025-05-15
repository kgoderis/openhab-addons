package org.openhab.io.homekit.library.service;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.service.HomekitServiceType;
import org.openhab.io.homekit.core.service.AbstractHomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.library.characteristic.HomekitCurrentTemperatureCharacteristic;

/**
 * Service that represents a temperature sensor in HomeKit.
 * This service provides information about temperature readings.
 *
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitServiceType(type = "0000008A-0000-1000-8000-0026BB765291", name = "Temperature Sensor", tag = "temperatureSensor")
@NonNullByDefault
public class HomekitTemperatureSensorService extends AbstractHomekitService {
    /**
     * Creates a new HomekitTemperatureSensorService.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     */
    public HomekitTemperatureSensorService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory) {
        super(accessory, eventManager, characteristicFactory);
        withName("Temperature Sensor").withPrimary(false).withHidden(false);
    }

    /**
     * Creates a new HomekitTemperatureSensorService from a JSON value.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @param value JSON value containing service configuration
     */
    public HomekitTemperatureSensorService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory, JsonValue value) {
        super(accessory, eventManager, characteristicFactory, value);
    }

    @Override
    public void addCharacteristics() {
        addCharacteristic(new HomekitCurrentTemperatureCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
    }

    @Override
    public boolean isExtensible() {
        return false;
    }
}
