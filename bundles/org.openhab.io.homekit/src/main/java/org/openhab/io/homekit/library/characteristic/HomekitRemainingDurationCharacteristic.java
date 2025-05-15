package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Remaining Duration Characteristic.
 * This characteristic represents the remaining duration for a device or service.
 *
 * @see <a href=\"https://developer.apple.com/documentation/HomeKit\">HAP Specification</a>
 * @author Karel Goderis - Initial Contribution
 */
@HomekitCharacteristicType(type = "000000D4-0000-1000-8000-0026BB765291", name = "Remaining Duration", tag = "remainingDuration", acceptedItemTypes = {
        "Number" })
@NonNullByDefault
public class HomekitRemainingDurationCharacteristic extends HomekitIntegerCharacteristic {

    public HomekitRemainingDurationCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, 0, 3600, "seconds");
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(true)
                .withDescription("Remaining Duration");
    }

    public HomekitRemainingDurationCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= 0 && value <= 3600;
    }
}
