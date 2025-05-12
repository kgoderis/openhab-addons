package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitBooleanCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Siri Touch To Use Characteristic.
 * This characteristic represents whether Siri can be activated by touch.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/SiriTouchToUse">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "00000257-0000-1000-8000-0026BB765291", name = "Siri Touch To Use", tag = "siriTouchToUse")
@NonNullByDefault
public class HomekitSiriTouchToUseCharacteristic extends HomekitBooleanCharacteristic {
    public HomekitSiriTouchToUseCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId)
            .withPairedRead(true)
            .withPairedWrite(true)
            .withEvents(true)
            .withDescription("Siri Touch To Use");
    }

    public HomekitSiriTouchToUseCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }
} 