package org.openhab.io.homekit.library.service;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.service.HomekitServiceType;
import org.openhab.io.homekit.core.service.AbstractHomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.library.characteristic.HomekitCarbonMonoxideDetectedCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitCarbonMonoxideLevelCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitCarbonMonoxidePeakLevelCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitNameCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitStatusActiveCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitStatusFaultCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitStatusLowBatteryCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitStatusTamperedCharacteristic;

/**
 * Service that represents a carbon monoxide sensor in HomeKit.
 * This service provides information about carbon monoxide levels and detection.
 *
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitServiceType(type = "0000007F-0000-1000-8000-0026BB765291", name = "Carbon Monoxide Sensor", tag = "carbonMonoxideSensor")
@NonNullByDefault
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
        withName("Carbon Monoxide Sensor").withPrimary(false).withHidden(false);
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
        addCharacteristic(new HomekitCarbonMonoxideDetectedCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        addCharacteristic(new HomekitCarbonMonoxideLevelCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(false));
        addCharacteristic(new HomekitCarbonMonoxidePeakLevelCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(false));
        addCharacteristic(new HomekitNameCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                .withMandatory(false));
        addCharacteristic(
                new HomekitStatusActiveCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(false));
        addCharacteristic(
                new HomekitStatusFaultCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(false));
        addCharacteristic(new HomekitStatusLowBatteryCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(false));
        addCharacteristic(
                new HomekitStatusTamperedCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(false));
    }

    @Override
    public boolean isExtensible() {
        return false;
    }
}
