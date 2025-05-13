package org.openhab.io.homekit.library.service;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.service.HomekitServiceType;
import org.openhab.io.homekit.core.service.AbstractHomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.library.characteristic.HomekitAirPlayEnableCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitConfiguredNameCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitCurrentMediaStateCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitMuteCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitNameCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitSmartSpeakerCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitTargetMediaStateCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitVolumeCharacteristic;

/**
 * HomeKit Smart Speaker Service.
 * This service provides smart speaker functionality in HomeKit.
 * For more information, see https://developer.apple.com/documentation/HomeKit
 *
 * @author Karel Goderis
 */
@HomekitServiceType(type = "00000228-0000-1000-8000-0026BB765291", name = "SmartSpeaker", tag = "smartSpeaker")
@NonNullByDefault
public class HomekitSmartSpeakerService extends AbstractHomekitService {

    /**
     * Creates a new Smart Speaker service.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     */
    public HomekitSmartSpeakerService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory) {
        super(accessory, eventManager, characteristicFactory);
        withName("Smart Speaker")
            .withExtensible(true)
            .withPrimary(false)
            .withHidden(false);
    }

    /**
     * Creates a new Smart Speaker service from a JSON configuration.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @param value JSON value containing service configuration
     */
    public HomekitSmartSpeakerService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory, JsonValue value) {
        super(accessory, eventManager, characteristicFactory, value);
    }

    /**
     * Adds the required characteristics for this service.
     * Required: SmartSpeaker
     */
    @Override
    public void addCharacteristics() {
        // Required characteristics
        addCharacteristic(new HomekitCurrentMediaStateCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        addCharacteristic(new HomekitTargetMediaStateCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        addCharacteristic(new HomekitAirPlayEnableCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(false));
                addCharacteristic(new HomekitConfiguredNameCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(false));
                addCharacteristic(new HomekitMuteCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(false));
                addCharacteristic(new HomekitNameCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(false));
                addCharacteristic(new HomekitVolumeCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(false));
    }
}
