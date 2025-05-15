package org.openhab.io.homekit.library.service;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.service.HomekitServiceType;
import org.openhab.io.homekit.core.service.AbstractHomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.library.characteristic.HomekitMuteCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitNameCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitVolumeCharacteristic;

/**
 * Service that represents a microphone in HomeKit.
 * This service provides control over microphone settings and operation.
 *
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitServiceType(type = "00000112-0000-1000-8000-0026BB765291", name = "Microphone", tag = "microphone")
@NonNullByDefault
public class HomekitMicrophoneService extends AbstractHomekitService {

    /**
     * Creates a new HomekitMicrophoneService.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     */
    public HomekitMicrophoneService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory) {
        super(accessory, eventManager, characteristicFactory);
        withName("Microphone").withPrimary(false).withHidden(false);
    }

    /**
     * Creates a new HomekitMicrophoneService from a JSON value.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @param value JSON value containing service configuration
     */
    public HomekitMicrophoneService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory, JsonValue value) {
        super(accessory, eventManager, characteristicFactory, value);
    }

    @Override
    public void addCharacteristics() {
        addCharacteristic(new HomekitMuteCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                .withMandatory(true));
        addCharacteristic(
                new HomekitVolumeCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(false));
        addCharacteristic(new HomekitNameCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                .withMandatory(false));
    }

    @Override
    public boolean isExtensible() {
        return false;
    }
}
