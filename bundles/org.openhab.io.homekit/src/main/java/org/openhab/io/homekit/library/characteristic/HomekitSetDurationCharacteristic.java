package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Set Duration Characteristic.
 * @see <a href="https://developers.homebridge.io/#/characteristic/SetDuration">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "000000D3-0000-1000-8000-0026BB765291", name = "Set Duration", tag = "setDuration")
@NonNullByDefault
public class HomekitSetDurationCharacteristic extends HomekitIntegerCharacteristic {

    public HomekitSetDurationCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 0, 3600, "seconds");
        withInstanceId(instanceId)
            .withPairedWrite(true)
            .withPairedRead(true)
            .withEvents(true)
            .withDescription("Set Duration");
    }

    public HomekitSetDurationCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= 0 && value <= 3600;
    }
} 