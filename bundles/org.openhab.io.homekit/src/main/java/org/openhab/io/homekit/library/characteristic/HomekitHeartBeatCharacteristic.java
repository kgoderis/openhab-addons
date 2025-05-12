package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Heart Beat Characteristic.
 * This characteristic represents the heart beat value.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/HeartBeat">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "0000024A-0000-1000-8000-0026BB765291", name = "Heart Beat", tag = "heartBeat")
@NonNullByDefault
public class HomekitHeartBeatCharacteristic extends HomekitIntegerCharacteristic {
    public HomekitHeartBeatCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 0, Integer.MAX_VALUE, "");
        withInstanceId(instanceId)
            .withPairedRead(true)
            .withPairedWrite(false)
            .withEvents(true)
            .withDescription("Heart Beat");
    }

    public HomekitHeartBeatCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= 0;
    }
} 