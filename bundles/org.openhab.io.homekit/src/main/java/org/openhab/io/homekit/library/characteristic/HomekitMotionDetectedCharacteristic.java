package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Motion Detected Characteristic.
 * This characteristic represents whether motion has been detected.
 * The value is an enumeration with two states: NOT_DETECTED and DETECTED.
 *
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/homekit/hap-characteristic-types/motion-detected">HAP
 *      Specification</a>
 */
@HomekitCharacteristicType(type = "00000022-0000-1000-8000-0026BB765291", name = "Motion Detected", tag = "motionDetected", acceptedItemTypes = {"Number", "String"})
@NonNullByDefault
public class HomekitMotionDetectedCharacteristic extends HomekitEnumCharacteristic {

    public enum MotionDetected {
        NOT_DETECTED(0),
        DETECTED(1);

        private final int value;

        MotionDetected(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static MotionDetected fromValue(int value) {
            for (MotionDetected state : values()) {
                if (state.value == value) {
                    return state;
                }
            }
            throw new IllegalArgumentException("Invalid Motion Detected value: " + value);
        }
    }

    /**
     * Creates a new Motion Detected characteristic.
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param instanceId The instance ID for this characteristic
     */
    public HomekitMotionDetectedCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, MotionDetected.values().length);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(false).withEvents(true)
                .withDescription("Motion Detected");
    }

    /**
     * Creates a new Motion Detected characteristic from a JSON value.
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param value The JSON value to initialize the characteristic with
     */
    public HomekitMotionDetectedCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null
                && (value == MotionDetected.NOT_DETECTED.getValue() || value == MotionDetected.DETECTED.getValue());
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(MotionDetected.NOT_DETECTED.getValue(), MotionDetected.DETECTED.getValue());
    }
}
