package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Event Retransmission Maximum Characteristic.
 * This characteristic represents the maximum number of event retransmissions.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/EventRetransmissionMaximum">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "0000023D-0000-1000-8000-0026BB765291", name = "Event Retransmission Maximum", tag = "eventRetransmissionMaximum")
@NonNullByDefault
public class HomekitEventRetransmissionMaximumCharacteristic extends HomekitIntegerCharacteristic {
    public HomekitEventRetransmissionMaximumCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 0, Integer.MAX_VALUE, "");
        withInstanceId(instanceId)
            .withPairedRead(true)
            .withPairedWrite(true)
            .withEvents(true)
            .withDescription("Event Retransmission Maximum");
    }

    public HomekitEventRetransmissionMaximumCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= 0;
    }
} 