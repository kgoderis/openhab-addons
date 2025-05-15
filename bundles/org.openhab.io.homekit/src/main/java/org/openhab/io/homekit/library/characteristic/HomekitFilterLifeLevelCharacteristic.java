package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitFloatCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Filter Life Level Characteristic.
 * This characteristic represents the remaining life of a filter as a percentage (0-100).
 *
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 * @author Karel Goderis - Initial contribution
 */
@HomekitCharacteristicType(type = "000000AB-0000-1000-8000-0026BB765291", name = "Filter Life Level", tag = "filterLifeLevel", acceptedItemTypes = {"Number"})
@NonNullByDefault
public class HomekitFilterLifeLevelCharacteristic extends HomekitFloatCharacteristic {
    /**
     * Creates a new Filter Life Level characteristic.
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param instanceId The instance ID for this characteristic
     */
    public HomekitFilterLifeLevelCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, 0.0, 100.0, 1.0, "percentage");
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(true)
                .withDescription("Filter Life Level");
    }

    /**
     * Creates a new Filter Life Level characteristic from a JSON value.
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param value The JSON value to initialize the characteristic with
     */
    public HomekitFilterLifeLevelCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    /**
     * Checks if the given value is a valid filter life level.
     *
     * @param value the double value to check
     * @return true if the value is between 0.0 and 100.0, false otherwise
     */
    @Override
    public boolean isAllowedValue(Double value) {
        return value != null && value >= 0.0 && value <= 100.0;
    }

    /**
     * Gets the set of all valid filter life level values (empty for continuous range).
     *
     * @return an empty set
     */
    @Override
    public java.util.Set<Double> getAllowedValues() {
        return java.util.Collections.emptySet();
    }
}
