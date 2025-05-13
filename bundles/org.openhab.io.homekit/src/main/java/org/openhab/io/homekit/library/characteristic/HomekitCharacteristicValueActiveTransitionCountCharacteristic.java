package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Characteristic Value Active Transition Count.
 * This characteristic represents the number of active transitions for a characteristic value.
 *
 * @author Karel Goderis - Initial Contribution
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "0000024B-0000-1000-8000-0026BB765291", name = "Characteristic Value Active Transition Count", tag = "characteristicValueActiveTransitionCount")
@NonNullByDefault
public class HomekitCharacteristicValueActiveTransitionCountCharacteristic extends HomekitIntegerCharacteristic {

    /**
     * Constructs a new Characteristic Value Active Transition Count characteristic.
     *
     * @param service the HomeKit service this characteristic belongs to
     * @param eventManager the event manager for handling HomeKit events
     * @param instanceId the instance ID for this characteristic
     */
    public HomekitCharacteristicValueActiveTransitionCountCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 0, 255, "");
        withInstanceId(instanceId)
            .withPairedRead(true)
            .withPairedWrite(false)
            .withEvents(true)
            .withDescription("Characteristic Value Active Transition Count");
    }

    /**
     * Constructs a new Characteristic Value Active Transition Count characteristic from a JSON value.
     *
     * @param service the HomeKit service this characteristic belongs to
     * @param eventManager the event manager for handling HomeKit events
     * @param value the JSON value to initialize the characteristic with
     */
    public HomekitCharacteristicValueActiveTransitionCountCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    /**
     * Checks if the given value is a valid active transition count (0-255).
     *
     * @param value the value to check
     * @return true if the value is between 0 and 255, false otherwise
     */
    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= 0 && value <= 255;
    }
} 