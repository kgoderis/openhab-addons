package org.openhab.io.homekit.library.service;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.service.HomekitServiceType;
import org.openhab.io.homekit.core.service.AbstractHomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.library.characteristic.HomekitActiveCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitConfiguredNameCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitSupportedAudioRecordingConfigurationCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitSupportedVideoRecordingConfigurationCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitSelectedAudioRecordingConfigurationCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitSelectedVideoRecordingConfigurationCharacteristic;

/**
 * Service that represents camera recording management in HomeKit.
 * This service provides control over camera recording settings and configurations.
 *
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitServiceType(type = "00000204-0000-1000-8000-0026BB765291", name = "Camera Recording Management", tag = "cameraRecordingManagement")
@NonNullByDefault
public class HomekitCameraRecordingManagementService extends AbstractHomekitService {

    /**
     * Creates a new HomekitCameraRecordingManagementService.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     */
    public HomekitCameraRecordingManagementService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory) {
        super(accessory, eventManager, characteristicFactory);
        withName("Camera Recording Management")
            .withExtensible(false)
            .withPrimary(false)
            .withHidden(false);
    }

    /**
     * Creates a new HomekitCameraRecordingManagementService from a JSON value.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @param value JSON value containing service configuration
     */
    public HomekitCameraRecordingManagementService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory, JsonValue value) {
        super(accessory, eventManager, characteristicFactory, value);
    }

    @Override
    public void addCharacteristics() {
        super.addCharacteristics();
        addCharacteristic(new HomekitActiveCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        addCharacteristic(new HomekitConfiguredNameCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        addCharacteristic(new HomekitSupportedAudioRecordingConfigurationCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        addCharacteristic(new HomekitSupportedVideoRecordingConfigurationCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        addCharacteristic(new HomekitSelectedAudioRecordingConfigurationCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        addCharacteristic(new HomekitSelectedVideoRecordingConfigurationCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
    }

    @Override
    public boolean isExtensible() {
        return false;
    }
} 