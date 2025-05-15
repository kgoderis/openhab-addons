package org.openhab.io.homekit.library.service;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.service.HomekitServiceType;
import org.openhab.io.homekit.core.service.AbstractHomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.library.characteristic.HomekitActiveCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitRecordingAudioActiveCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitSelectedCameraRecordingConfigurationCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitSupportedAudioRecordingConfigurationCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitSupportedCameraRecordingConfigurationCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitSupportedVideoRecordingConfigurationCharacteristic;

/**
 * HomeKit Camera Recording Management Service.
 * This service provides control over camera recording settings and configurations.
 * For more information, see https://developer.apple.com/documentation/HomeKit
 *
 * @author Karel Goderis
 */
@HomekitServiceType(type = "00000204-0000-1000-8000-0026BB765291", name = "CameraRecordingManagement", tag = "cameraRecordingManagement")
@NonNullByDefault
public class HomekitCameraRecordingManagementService extends AbstractHomekitService {

    /**
     * Creates a new Camera Recording Management service.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     */
    public HomekitCameraRecordingManagementService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory) {
        super(accessory, eventManager, characteristicFactory);
        withName("Camera Recording Management").withExtensible(true).withPrimary(false).withHidden(false);
    }

    /**
     * Creates a new Camera Recording Management service from a JSON configuration.
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

    /**
     * Adds the required and optional characteristics for this service.
     * Required: Active, SelectedCameraRecordingConfiguration, SupportedAudioRecordingConfiguration,
     * SupportedCameraRecordingConfiguration, SupportedVideoRecordingConfiguration
     * Optional: RecordingAudioActive
     */
    @Override
    public void addCharacteristics() {
        // Required characteristics
        addCharacteristic(
                new HomekitActiveCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(true));
        addCharacteristic(new HomekitSelectedCameraRecordingConfigurationCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        addCharacteristic(new HomekitSupportedAudioRecordingConfigurationCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        addCharacteristic(new HomekitSupportedCameraRecordingConfigurationCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        addCharacteristic(new HomekitSupportedVideoRecordingConfigurationCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));

        // Optional characteristics
        addCharacteristic(new HomekitRecordingAudioActiveCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(false));
    }
}
