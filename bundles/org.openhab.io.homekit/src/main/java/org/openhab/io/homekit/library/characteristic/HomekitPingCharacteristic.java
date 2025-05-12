package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Ping Characteristic.
 * This characteristic represents the ping value in milliseconds.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/Ping">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "0000023C-0000-1000-8000-0026BB765291", name = "Ping", tag = "ping")
@NonNullByDefault
public class HomekitPingCharacteristic extends HomekitIntegerCharacteristic {
    public HomekitPingCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 0, Integer.MAX_VALUE, "ms");
        withInstanceId(instanceId)
            .withPairedRead(true)
            .withPairedWrite(false)
            .withEvents(true)
            .withDescription("Ping");
    }

    public HomekitPingCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= 0;
    }
} 