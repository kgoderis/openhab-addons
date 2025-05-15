package org.openhab.io.homekit.library.service;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.service.HomekitServiceType;
import org.openhab.io.homekit.core.service.AbstractHomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.library.characteristic.HomekitFirmwareUpdateReadinessCharacteristic;

/**
 * HomeKit Firmware Update Service.
 * This service provides functionality for managing firmware updates in HomeKit.
 * For more information, see https://developer.apple.com/documentation/HomeKit
 *
 * @author Karel Goderis
 */
@HomekitServiceType(type = "00000236-0000-1000-8000-0026BB765291", name = "FirmwareUpdate", tag = "firmwareUpdate")
@NonNullByDefault
public class HomekitFirmwareUpdateService extends AbstractHomekitService {

    /**
     * Creates a new Firmware Update service.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     */
    public HomekitFirmwareUpdateService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory) {
        super(accessory, eventManager, characteristicFactory);
        withName("Firmware Update").withExtensible(true).withPrimary(false).withHidden(false);
    }

    /**
     * Creates a new Firmware Update service from a JSON configuration.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @param value JSON value containing service configuration
     */
    public HomekitFirmwareUpdateService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory, JsonValue value) {
        super(accessory, eventManager, characteristicFactory, value);
    }

    /**
     * Adds the required characteristics for this service.
     * Required: FirmwareUpdateReadiness
     */
    @Override
    public void addCharacteristics() {
        // Required characteristics
        addCharacteristic(new HomekitFirmwareUpdateReadinessCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
    }
}
