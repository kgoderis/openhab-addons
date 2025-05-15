package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Pairing Features Characteristic.
 * This characteristic represents the pairing features supported by the accessory.
 * It is a read-only integer value that indicates the capabilities and features available during pairing.
 *
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(
    type = "0000004F-0000-1000-8000-0026BB765291",
    name = "Pairing Features",
    tag = "pairingFeatures",
    acceptedItemTypes = {"Number"}
)
@NonNullByDefault
public class HomekitPairingFeaturesCharacteristic extends HomekitIntegerCharacteristic {

    /**
     * Creates a new Pairing Features characteristic.
     * The value range is 0 to Integer.MAX_VALUE.
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param instanceId The instance ID for this characteristic
     */
    public HomekitPairingFeaturesCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, 0, Integer.MAX_VALUE, "");
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(true)
                .withDescription("Pairing Features");
    }

    /**
     * Creates a new Pairing Features characteristic from a JSON value.
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param value The JSON value to initialize the characteristic with
     */
    public HomekitPairingFeaturesCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    /**
     * Checks if the given value is allowed for this characteristic.
     * The value must be non-null and greater than or equal to 0.
     *
     * @param value The value to check
     * @return true if the value is allowed, false otherwise
     */
    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= 0;
    }

    /**
     * Gets the set of allowed values for this characteristic.
     * Since this characteristic accepts any non-negative integer, returns an empty set.
     *
     * @return An empty set, indicating any non-negative integer is allowed
     */
    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Collections.emptySet();
    }
}
