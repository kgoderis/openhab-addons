package org.openhab.io.homekit.library.service;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.service.HomekitServiceType;
import org.openhab.io.homekit.core.service.AbstractHomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.library.characteristic.HomekitCameraOperatingModeIndicatorCharacteristic;

/**
 * HomeKit Camera Operating Mode Service.
 * This service provides functionality for controlling camera operating modes in HomeKit.
 * For more information, see https://developer.apple.com/documentation/HomeKit
 *
 * @author Karel Goderis
 */
@HomekitServiceType(type = "0000021A-0000-1000-8000-0026BB765291", name = "CameraOperatingMode", tag = "cameraOperatingMode")
@NonNullByDefault
public class HomekitCameraOperatingModeService extends AbstractHomekitService {

    /**
     * Creates a new Camera Operating Mode service.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     */
    public HomekitCameraOperatingModeService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory) {
        super(accessory, eventManager, characteristicFactory);
        withName("Camera Operating Mode")
            .withExtensible(true)
            .withPrimary(false)
            .withHidden(false);
    }

    /**
     * Creates a new Camera Operating Mode service from a JSON configuration.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @param value JSON value containing service configuration
     */
    public HomekitCameraOperatingModeService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory, JsonValue value) {
        super(accessory, eventManager, characteristicFactory, value);
    }

    /**
     * Adds the required characteristics for this service.
     * Required: CameraOperatingModeIndicator
     */
    @Override
    public void addCharacteristics() {
        // Required characteristics
        addCharacteristic(new HomekitCameraOperatingModeIndicatorCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
    }
}
