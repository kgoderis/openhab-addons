package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Target Relative Humidity Characteristic.
 * This characteristic represents the target relative humidity for a device.
 * The humidity is expressed as a percentage, ranging from 0% to 100%.
 * This is used to set the desired relative humidity level for a humidifier or dehumidifier.
 *
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "000000C4-0000-1000-8000-0026BB765291", name = "Target Relative Humidity", tag = "targetRelativeHumidity")
@NonNullByDefault
public class HomekitTargetRelativeHumidityCharacteristic extends HomekitIntegerCharacteristic {

    /**
     * Creates a new Target Relative Humidity characteristic.
     * The value range is 0-100% with 0.1% step.
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param instanceId The instance ID for this characteristic
     */
    public HomekitTargetRelativeHumidityCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 0, 100, "percentage");
        withInstanceId(instanceId)
            .withPairedRead(true)
            .withPairedWrite(true)
            .withEvents(true)
            .withDescription("Target Relative Humidity");
    }

    /**
     * Creates a new Target Relative Humidity characteristic from a JSON value.
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param value The JSON value to initialize the characteristic with
     */
    public HomekitTargetRelativeHumidityCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
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