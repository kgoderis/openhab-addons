package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Siri Input Type Characteristic.
 * This characteristic represents the Siri input type for a device.
 *
 * @see <a href=\"https://developer.apple.com/documentation/HomeKit\">HAP Specification</a>
 * @author Karel Goderis - Initial Contribution
 */
@HomekitCharacteristicType(type = "00000132-0000-1000-8000-0026BB765291", name = "Siri Input Type", tag = "siriInputType", acceptedItemTypes = {"Number"})
@NonNullByDefault
public class HomekitSiriInputTypeCharacteristic extends HomekitIntegerCharacteristic {
    public static final int PUSH_BUTTON_TRIGGERED_APPLE_TV = 0;

    public HomekitSiriInputTypeCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, 0, 0, "");
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(false).withDescription("Siri Input Type");
    }

    public HomekitSiriInputTypeCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value == PUSH_BUTTON_TRIGGERED_APPLE_TV;
    }
}
