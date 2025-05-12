package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Smoke Detected Characteristic.
 * This characteristic represents whether smoke has been detected.
 * The value is an enumeration with two states: NOT_DETECTED and DETECTED.
 *
 * @see <a href="https://developer.apple.com/documentation/homekit/hmcharacteristictypesmokedetected">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "00000076-0000-1000-8000-0026BB765291", name = "Smoke Detected", tag = "smokeDetected")
@NonNullByDefault
public class HomekitSmokeDetectedCharacteristic extends HomekitEnumCharacteristic {

    public enum SmokeDetected {
        NOT_DETECTED(0),
        DETECTED(1);

        private final int code;

        SmokeDetected(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static SmokeDetected fromCode(int code) {
            for (SmokeDetected state : values()) {
                if (state.code == code) {
                    return state;
                }
            }
            return NOT_DETECTED;
        }
    }

    /**
     * Creates a new Smoke Detected characteristic.
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param instanceId The instance ID for this characteristic
     */
    public HomekitSmokeDetectedCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, SmokeDetected.values().length);
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(true)
            .withDescription("Smoke Detected");
    }

    /**
     * Creates a new Smoke Detected characteristic from a JSON value.
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param value The JSON value to initialize the characteristic with
     */
    public HomekitSmokeDetectedCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && (value == SmokeDetected.NOT_DETECTED.getCode() || 
                                value == SmokeDetected.DETECTED.getCode());
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(SmokeDetected.NOT_DETECTED.getCode(), 
                              SmokeDetected.DETECTED.getCode());
    }
} 