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
 * @see <a href="https://developer.apple.com/documentation/homekit/hmcharacteristictypemotiondetected">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "00000022-0000-1000-8000-0026BB765291", name = "Motion Detected", tag = "motionDetected")
@NonNullByDefault
public class HomekitMotionDetectedCharacteristic extends HomekitEnumCharacteristic {

    public enum MotionDetected {
        NOT_DETECTED(0),
        DETECTED(1);

        private final int code;

        MotionDetected(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static MotionDetected fromCode(int code) {
            for (MotionDetected state : values()) {
                if (state.code == code) {
                    return state;
                }
            }
            return NOT_DETECTED;
        }
    }

    /**
     * Creates a new Motion Detected characteristic.
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param instanceId The instance ID for this characteristic
     */
    public HomekitMotionDetectedCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, MotionDetected.values().length);
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(true)
            .withDescription("Motion Detected");
    }

    /**
     * Creates a new Motion Detected characteristic from a JSON value.
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param value The JSON value to initialize the characteristic with
     */
    public HomekitMotionDetectedCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && (value == MotionDetected.NOT_DETECTED.getCode() || 
                                value == MotionDetected.DETECTED.getCode());
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(MotionDetected.NOT_DETECTED.getCode(), 
                              MotionDetected.DETECTED.getCode());
    }
} 