package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitLongCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Accessory Flags Characteristic.
 * This characteristic represents the flags that describe the capabilities and state of an accessory.
 * The flags are represented as a 32-bit unsigned integer (0 to 0xFFFFFFFF).
 *
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "000000A6-0000-1000-8000-0026BB765291", name = "Accessory Flags", tag = "accessoryFlags", acceptedItemTypes = {"Number"})
@NonNullByDefault
public class HomekitAccessoryFlagsCharacteristic extends HomekitLongCharacteristic {

    public HomekitAccessoryFlagsCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, 0L, 0xFFFFFFFFL, 1L);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(false).withEvents(true)
                .withDescription("Accessory Flags");
    }

    public HomekitAccessoryFlagsCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Long value) {
        return value != null && value >= 0L && value <= 0xFFFFFFFFL;
    }

    @Override
    public java.util.Set<Long> getAllowedValues() {
        return java.util.Collections.emptySet(); // No specific allowed values, just a range
    }
}
