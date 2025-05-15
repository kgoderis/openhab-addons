package org.openhab.io.homekit.library.service;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.service.HomekitServiceType;
import org.openhab.io.homekit.core.service.AbstractHomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.library.characteristic.HomekitSelectedRTPStreamConfigurationCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitSetupEndpointsCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitStreamingStatusCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitSupportedAudioStreamConfigurationCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitSupportedRTPConfigurationCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitSupportedVideoStreamConfigurationCharacteristic;

/**
 * Service that represents camera RTP stream management in HomeKit.
 * This service provides control over camera streaming settings and configurations.
 *
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitServiceType(type = "00000120-0000-1000-8000-0026BB765291", name = "Camera RTP Stream Management", tag = "cameraRTPStreamManagement")
@NonNullByDefault
public class HomekitCameraRTPStreamManagementService extends AbstractHomekitService {

    /**
     * Creates a new HomekitCameraRTPStreamManagementService.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     */
    public HomekitCameraRTPStreamManagementService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory) {
        super(accessory, eventManager, characteristicFactory);
        withName("Camera RTP Stream Management").withPrimary(false).withHidden(false);
    }

    /**
     * Creates a new HomekitCameraRTPStreamManagementService from a JSON value.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @param value JSON value containing service configuration
     */
    public HomekitCameraRTPStreamManagementService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory, JsonValue value) {
        super(accessory, eventManager, characteristicFactory, value);
    }

    @Override
    public void addCharacteristics() {
        addCharacteristic(new HomekitStreamingStatusCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        addCharacteristic(new HomekitSupportedVideoStreamConfigurationCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        addCharacteristic(new HomekitSupportedAudioStreamConfigurationCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        addCharacteristic(new HomekitSupportedRTPConfigurationCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        addCharacteristic(new HomekitSelectedRTPStreamConfigurationCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        addCharacteristic(
                new HomekitSetupEndpointsCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(true));
    }

    @Override
    public boolean isExtensible() {
        return false;
    }
}
