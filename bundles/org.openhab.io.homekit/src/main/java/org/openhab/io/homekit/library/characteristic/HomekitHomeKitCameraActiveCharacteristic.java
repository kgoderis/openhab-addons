package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import java.util.Set;

/**
 * HomeKit Camera Active Characteristic.
 * This characteristic represents whether the HomeKit camera is active or not.
 *
 * @see <a href=\"https://developer.apple.com/documentation/HomeKit\">HAP Specification</a>
 * @author Karel Goderis - Initial Contribution
 */
@HomekitCharacteristicType(type = "0000021B-0000-1000-8000-0026BB765291", name = "HomeKit Camera Active", tag = "homeKitCameraActive")
@NonNullByDefault
public class HomekitHomeKitCameraActiveCharacteristic extends HomekitIntegerCharacteristic {
    /**
     * Enum representing the possible states for the camera.
     */
    public enum CameraActiveState {
        OFF(0),
        ON(1);
        private final int code;
        CameraActiveState(int code) { this.code = code; }
        public int getCode() { return code; }
        public static CameraActiveState fromCode(int code) {
            for (CameraActiveState s : values()) {
                if (s.code == code) return s;
            }
            return OFF;
        }
    }

    public HomekitHomeKitCameraActiveCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 0, 1, "");
        withInstanceId(instanceId)
            .withPairedRead(true)
            .withPairedWrite(true)
            .withEvents(true)
            .withDescription("HomeKit Camera Active");
    }

    public HomekitHomeKitCameraActiveCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    /**
     * Checks if the given value is a valid camera active state.
     *
     * @param value the value to check
     * @return true if the value is OFF or ON, false otherwise
     */
    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && (value == CameraActiveState.OFF.getCode() || value == CameraActiveState.ON.getCode());
    }

    /**
     * Returns the set of allowed camera active state values.
     *
     * @return a set containing OFF and ON
     */
    @Override
    public Set<Integer> getAllowedValues() {
        return Set.of(CameraActiveState.OFF.getCode(), CameraActiveState.ON.getCode());
    }
} 