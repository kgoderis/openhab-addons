package org.openhab.io.homekit.library.service;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.service.HomekitServiceType;
import org.openhab.io.homekit.core.service.AbstractHomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.library.characteristic.HomekitContactSensorStateCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitNameCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitStatusActiveCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitStatusFaultCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitStatusLowBatteryCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitStatusTamperedCharacteristic;

/**
 * Service that represents a contact sensor in HomeKit.
 * This service provides information about contact sensor state and status.
 * 
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitServiceType(type = "00000080-0000-1000-8000-0026BB765291", name = "Contact Sensor", tag = "contactSensor")
@NonNullByDefault
public class HomekitContactSensorService extends AbstractHomekitService {

    /**
     * Creates a new HomekitContactSensorService.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     */
    public HomekitContactSensorService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory) {
        super(accessory, eventManager, characteristicFactory);
        withName("Contact Sensor").withPrimary(false).withHidden(false);
    }

    /**
     * Creates a new HomekitContactSensorService from a JSON value.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @param value JSON value containing service configuration
     */
    public HomekitContactSensorService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory, JsonValue value) {
        super(accessory, eventManager, characteristicFactory, value);
    }

    @Override
    public void addCharacteristics() {
        addCharacteristic(new HomekitContactSensorStateCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
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
