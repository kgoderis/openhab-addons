package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit MAC Retransmission Maximum Characteristic.
 * This characteristic represents the maximum number of MAC retransmissions.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/MACRetransmissionMaximum">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "00000247-0000-1000-8000-0026BB765291", name = "MAC Retransmission Maximum", tag = "macRetransmissionMaximum", acceptedItemTypes = {
        "Number" })
@NonNullByDefault
public class HomekitMACRetransmissionMaximumCharacteristic extends HomekitIntegerCharacteristic {
    public HomekitMACRetransmissionMaximumCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, 0, Integer.MAX_VALUE, "");
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(true).withEvents(true)
                .withDescription("MAC Retransmission Maximum");
    }

    public HomekitMACRetransmissionMaximumCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= 0;
    }
}
