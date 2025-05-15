package org.openhab.io.homekit.library.service;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.service.HomekitServiceType;
import org.openhab.io.homekit.core.service.AbstractHomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.library.characteristic.HomekitActivityIntervalCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitHeartBeatCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitPingCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitSleepIntervalCharacteristic;

/**
 * HomeKit Accessory Runtime Information Service.
 * This service provides runtime information about the accessory.
 * For more information, see https://developer.apple.com/documentation/HomeKit
 *
 * @author Karel Goderis
 */
@HomekitServiceType(type = "00000239-0000-1000-8000-0026BB765291", name = "AccessoryRuntimeInformation", tag = "accessoryRuntimeInformation")
@NonNullByDefault
public class HomekitAccessoryRuntimeInformationService extends AbstractHomekitService {

    /**
     * Creates a new Accessory Runtime Information service.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     */
    public HomekitAccessoryRuntimeInformationService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory) {
        super(accessory, eventManager, characteristicFactory);
        withName("Accessory Runtime Information").withExtensible(true).withPrimary(false).withHidden(false);
    }

    /**
     * Creates a new Accessory Runtime Information service from a JSON configuration.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @param value JSON value containing service configuration
     */
    public HomekitAccessoryRuntimeInformationService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory, JsonValue value) {
        super(accessory, eventManager, characteristicFactory, value);
    }

    /**
     * Adds the required and optional characteristics for this service.
     * Required: Ping
     * Optional: ActivityInterval, HeartBeat
     */
    @Override
    public void addCharacteristics() {
        // Required characteristics
        addCharacteristic(new HomekitPingCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                .withMandatory(true));

        // Optional characteristics
        addCharacteristic(new HomekitActivityIntervalCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(false));
        addCharacteristic(
                new HomekitHeartBeatCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(false));
        addCharacteristic(
                new HomekitSleepIntervalCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(false));
    }
}
