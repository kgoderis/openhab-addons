package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Reset Filter Indication Characteristic.
 * This characteristic represents the indication to reset a filter, typically set to 1 to trigger a reset.
 *
 * @see <a href=\"https://developer.apple.com/documentation/HomeKit\">HAP Specification</a>
 * @author Karel Goderis - Initial Contribution
 */
@HomekitCharacteristicType(type = "000000AD-0000-1000-8000-0026BB765291", name = "Reset Filter Indication", tag = "resetFilterIndication")
@NonNullByDefault
public class HomekitResetFilterIndicationCharacteristic extends HomekitIntegerCharacteristic {

    public HomekitResetFilterIndicationCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 1, 1, "");
        withInstanceId(instanceId)
            .withPairedWrite(true)
            .withPairedRead(false)
            .withEvents(false)
            .withDescription("Reset Filter Indication");
    }

    public HomekitResetFilterIndicationCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    /**
     * Checks if the given value is a valid reset filter indication.
     *
     * @param value the value to check
     * @return true if the value is 1, false otherwise
     */
    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value == 1;
    }

    /**
     * Returns the set of allowed values for reset filter indication.
     *
     * @return a set containing only the value 1
     */
    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(1);
    }
} 