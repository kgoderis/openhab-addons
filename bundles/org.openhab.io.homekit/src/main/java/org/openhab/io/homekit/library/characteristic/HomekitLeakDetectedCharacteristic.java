package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * Leak Detected characteristic.
 * <p>
 * This characteristic represents whether a leak has been detected by the accessory.
 *
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 * @author Karel Goderis - Initial contribution
 */
@HomekitCharacteristicType(type = "00000070-0000-1000-8000-0026BB765291", name = "Leak Detected", tag = "leakDetected", acceptedItemTypes = {
        "Number", "String" })
@NonNullByDefault
public class HomekitLeakDetectedCharacteristic extends HomekitEnumCharacteristic {

    public enum LeakDetected {
        LEAK_NOT_DETECTED(0),
        LEAK_DETECTED(1);

        private final int code;

        LeakDetected(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static LeakDetected fromCode(int code) {
            for (LeakDetected state : values()) {
                if (state.code == code) {
                    return state;
                }
            }
            return LEAK_NOT_DETECTED;
        }
    }

    public HomekitLeakDetectedCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, LeakDetected.values().length);
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(true)
                .withDescription("Leak Detected");
    }

    public HomekitLeakDetectedCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null
                && (value == LeakDetected.LEAK_NOT_DETECTED.getCode() || value == LeakDetected.LEAK_DETECTED.getCode());
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(LeakDetected.LEAK_NOT_DETECTED.getCode(), LeakDetected.LEAK_DETECTED.getCode());
    }
}
