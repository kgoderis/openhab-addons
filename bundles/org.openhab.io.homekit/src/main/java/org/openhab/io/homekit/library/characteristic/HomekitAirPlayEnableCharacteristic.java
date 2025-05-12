package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitBooleanCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit AirPlay Enable Characteristic.
 * This characteristic represents whether AirPlay is enabled.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/AirPlayEnable">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "0000025B-0000-1000-8000-0026BB765291", name = "AirPlay Enable", tag = "airPlayEnable")
@NonNullByDefault
public class HomekitAirPlayEnableCharacteristic extends HomekitBooleanCharacteristic {
    public HomekitAirPlayEnableCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId)
            .withPairedRead(true)
            .withPairedWrite(true)
            .withEvents(true)
            .withDescription("AirPlay Enable");
    }

    public HomekitAirPlayEnableCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }
} 