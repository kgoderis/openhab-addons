package org.openhab.io.homekit.library.service;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.service.HomekitServiceType;
import org.openhab.io.homekit.core.service.AbstractHomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.library.characteristic.HomekitAccessoryFlagsCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitFirmwareRevisionCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitHardwareRevisionCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitIdentifyCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitManufacturerCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitModelCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitNameCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitSerialNumberCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitVersionCharacteristic;

/**
 * HomeKit Accessory Information Service.
 * This service provides basic information about the accessory, such as manufacturer, model, and serial number.
 * For more information, see https://developer.apple.com/documentation/HomeKit
 *
 * @author Karel Goderis
 */
@HomekitServiceType(type = "0000003E-0000-1000-8000-0026BB765291", name = "Accessory Information", tag = "accessoryInformation")
@NonNullByDefault
public class HomekitAccessoryInformationService extends AbstractHomekitService {

    /**
     * Creates a new Accessory Information service.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     */
    public HomekitAccessoryInformationService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory) {
        super(accessory, eventManager, characteristicFactory);
        withName("Accessory Information")
            .withExtensible(false)
            .withPrimary(false)
            .withHidden(false);
    }

    /**
     * Creates a new Accessory Information service from a JSON configuration.
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

    /**
     * Adds the required and optional characteristics for this service.
     * Required: Identify, Manufacturer, Model, Name, Serial Number, Version
     * Optional: Firmware Revision, Hardware Revision, Accessory Flags
     */
    @Override
    public void addCharacteristics() {
        // Required characteristics
        addCharacteristic(new HomekitIdentifyCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        addCharacteristic(new HomekitManufacturerCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        addCharacteristic(new HomekitModelCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        addCharacteristic(new HomekitNameCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        addCharacteristic(new HomekitSerialNumberCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        addCharacteristic(new HomekitVersionCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));

        // Optional characteristics
        addCharacteristic(new HomekitFirmwareRevisionCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(false));
        addCharacteristic(new HomekitHardwareRevisionCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(false));
        addCharacteristic(new HomekitAccessoryFlagsCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(false));
    }

    /**
     * Returns whether this service is extensible.
     * Accessory Information service is not extensible by default.
     *
     * @return false as this service is not extensible
     */
    @Override
    public boolean isExtensible() {
        return false;
    }
}
