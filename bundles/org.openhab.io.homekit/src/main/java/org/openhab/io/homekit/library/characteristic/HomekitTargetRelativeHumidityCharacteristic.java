package org.openhab.io.homekit.library.characteristic;

import java.util.Collections;
import java.util.Set;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitFloatCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Target Relative Humidity Characteristic.
 * This characteristic represents the target relative humidity level, as defined in the HomeKit Accessory Protocol (HAP)
 * specification.
 *
 * @author Karel Goderis - Initial Contribution
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "00000034-0000-1000-8000-0026BB765291", name = "Target Relative Humidity", tag = "targetRelativeHumidity", acceptedItemTypes = {
        "Number" })
@NonNullByDefault
public class HomekitTargetRelativeHumidityCharacteristic extends HomekitFloatCharacteristic {

    /**
     * Constructs a new Target Relative Humidity characteristic.
     *
     * @param service the HomeKit service this characteristic belongs to
     * @param eventManager the event manager for handling HomeKit events
     * @param instanceId the instance ID for this characteristic
     */
    public HomekitTargetRelativeHumidityCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, 0.0, 100.0, 1.0, "percentage");
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(true).withEvents(true)
                .withDescription("Target Relative Humidity");
    }

    /**
     * Constructs a new Target Relative Humidity characteristic from a JSON value.
     *
     * @param service the HomeKit service this characteristic belongs to
     * @param eventManager the event manager for handling HomeKit events
     * @param value the JSON value to initialize the characteristic with
     */
    public HomekitTargetRelativeHumidityCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    /**
     * Checks if the given value is within the allowed range (0-100%).
     *
     * @param value the value to check
     * @return true if the value is within range, false otherwise
     */
    @Override
    public boolean isAllowedValue(Double value) {
        return value != null && value >= 0.0 && value <= 100.0;
    }

    /**
     * Returns an empty set as there are no specific allowed values.
     *
     * @return an empty set
     */
    @Override
    public Set<Double> getAllowedValues() {
        return Collections.emptySet();
    }
}
