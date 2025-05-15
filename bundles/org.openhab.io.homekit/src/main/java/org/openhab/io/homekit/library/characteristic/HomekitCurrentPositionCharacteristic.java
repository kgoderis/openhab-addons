package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * Current Position characteristic.
 * This characteristic represents the current position of a door, window, or window covering (0-100%).
 *
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 * @author Karel Goderis - Initial contribution
 */
@HomekitCharacteristicType(type = "0000006D-0000-1000-8000-0026BB765291", name = "Current Position", tag = "currentPosition", acceptedItemTypes = {"Number", "Rollershutter"})
@NonNullByDefault
public class HomekitCurrentPositionCharacteristic extends HomekitIntegerCharacteristic {

    /**
     * Creates a new Current Position characteristic.
     * The value range is 0-100 (percent).
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param instanceId The instance ID for this characteristic
     */
    public HomekitCurrentPositionCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, 0, 100, "%");
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(true)
                .withDescription("Current Position");
    }

    /**
     * Creates a new Current Position characteristic from a JSON value.
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param value The JSON value to initialize the characteristic with
     */
    public HomekitCurrentPositionCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= 0 && value <= 100;
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Collections.emptySet(); // No specific allowed values, just a range
    }
}
