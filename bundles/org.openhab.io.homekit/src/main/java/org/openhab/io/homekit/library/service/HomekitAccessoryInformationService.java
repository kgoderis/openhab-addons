package org.openhab.io.homekit.library.service;

import javax.json.JsonValue;

import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.core.service.AbstractHomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.library.characteristic.HomekitFirmwareRevisionCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitIdentifyCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitManufacturerCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitModelCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitNameCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitSerialNumberCharacteristic;

/**
 * Service that provides accessory information like manufacturer, model, serial number etc.
 * This is a required service for all HomeKit accessories.
 */
public class HomekitAccessoryInformationService extends AbstractHomekitService {

    /**
     * Creates a new HomekitAccessoryInformationService.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     */
    public HomekitAccessoryInformationService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory) {
        super(accessory, eventManager, characteristicFactory);
    }

    /**
     * Creates a new HomekitAccessoryInformationService from a JSON value.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @param value JSON value containing service configuration
     */
    public HomekitAccessoryInformationService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory, JsonValue value) {
        super(accessory, eventManager, characteristicFactory, value);
    }

    @Override
    public void addCharacteristics() {
        super.addCharacteristics();
        addCharacteristic(new HomekitIdentifyCharacteristic(this,
                ((HomekitAccessory) getAccessory()).getNextAvailableInstanceId(), eventManager));
        addCharacteristic(new HomekitManufacturerCharacteristic(this,
                ((HomekitAccessory) getAccessory()).getNextAvailableInstanceId(), eventManager));
        addCharacteristic(new HomekitModelCharacteristic(this,
                ((HomekitAccessory) getAccessory()).getNextAvailableInstanceId(), eventManager));
        addCharacteristic(new HomekitNameCharacteristic(this,
                ((HomekitAccessory) getAccessory()).getNextAvailableInstanceId(), eventManager));
        addCharacteristic(new HomekitSerialNumberCharacteristic(this,
                ((HomekitAccessory) getAccessory()).getNextAvailableInstanceId(), eventManager));
        addCharacteristic(new HomekitFirmwareRevisionCharacteristic(this,
                ((HomekitAccessory) getAccessory()).getNextAvailableInstanceId(), eventManager));
    }

    @Override
    public boolean isExtensible() {
        return false;
    }
}
