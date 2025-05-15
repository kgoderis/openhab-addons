package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitFloatCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Battery Level Characteristic.
 * This characteristic represents the battery level as a percentage (0-100).
 *
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 * @author Karel Goderis - Initial contribution
 */
@HomekitCharacteristicType(type = "00000068-0000-1000-8000-0026BB765291", name = "Battery Level", tag = "batteryLevel", acceptedItemTypes = {"Number"})
@NonNullByDefault
public class HomekitBatteryLevelCharacteristic extends HomekitFloatCharacteristic {
    /**
     * Creates a new Battery Level characteristic.
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param instanceId The instance ID for this characteristic
     */
    public HomekitBatteryLevelCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, 0.0, 100.0, 1.0, "percentage");
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(true)
                .withDescription("Battery Level");
    }

    /**
     * Creates a new Battery Level characteristic from a JSON value.
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param value The JSON value to initialize the characteristic with
     */
    public HomekitBatteryLevelCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    /**
     * Checks if the given value is a valid battery level.
     *
     * @param value the double value to check
     * @return true if the value is between 0.0 and 100.0, false otherwise
     */
    @Override
    public boolean isAllowedValue(Double value) {
        return value != null && value >= 0.0 && value <= 100.0;
    }

    /**
     * Gets the set of all valid battery level values (empty for continuous range).
     *
     * @return an empty set
     */
    @Override
    public java.util.Set<Double> getAllowedValues() {
        return java.util.Collections.emptySet();
    }
}
