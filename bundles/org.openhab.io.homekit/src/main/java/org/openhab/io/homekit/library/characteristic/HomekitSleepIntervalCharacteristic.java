package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Sleep Interval Characteristic.
 * This characteristic represents the sleep interval in seconds.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/SleepInterval">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "0000023A-0000-1000-8000-0026BB765291", name = "Sleep Interval", tag = "sleepInterval")
@NonNullByDefault
public class HomekitSleepIntervalCharacteristic extends HomekitIntegerCharacteristic {
    public HomekitSleepIntervalCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 0, Integer.MAX_VALUE, "seconds");
        withInstanceId(instanceId)
            .withPairedRead(true)
            .withPairedWrite(true)
            .withEvents(true)
            .withDescription("Sleep Interval");
    }

    public HomekitSleepIntervalCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= 0;
    }
} 