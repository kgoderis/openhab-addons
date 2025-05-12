package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitStringCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Token Characteristic.
 * This characteristic represents an authentication token.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/Token">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "00000231-0000-1000-8000-0026BB765291", name = "Token", tag = "token")
@NonNullByDefault
public class HomekitTokenCharacteristic extends HomekitStringCharacteristic {
    public HomekitTokenCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId)
            .withPairedRead(true)
            .withPairedWrite(true)
            .withEvents(true)
            .withDescription("Token");
    }

    public HomekitTokenCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }
} 