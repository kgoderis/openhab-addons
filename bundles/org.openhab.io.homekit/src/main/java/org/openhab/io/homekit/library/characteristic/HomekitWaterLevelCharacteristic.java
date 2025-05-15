package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Water Level Characteristic.
 * This characteristic represents the water level for a device.
 * The level is expressed as a percentage, ranging from 0% to 100%.
 * This is used to indicate the current water level in devices like washing machines, dishwashers, or water tanks.
 *
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "000000B5-0000-1000-8000-0026BB765291", name = "Water Level", tag = "waterLevel", acceptedItemTypes = {"Number"})
@NonNullByDefault
public class HomekitWaterLevelCharacteristic extends HomekitIntegerCharacteristic {
    /**
     * Creates a new Water Level characteristic.
     * The value range is 0 to 100 percent.
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param instanceId The instance ID for this characteristic
     */
    public HomekitWaterLevelCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 0, 100, "percentage");
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(false).withEvents(true)
                .withDescription("Water Level");
    }

    /**
     * Creates a new Water Level characteristic from a JSON value.
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param value The JSON value to initialize the characteristic with
     */
    public HomekitWaterLevelCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
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
