package org.openhab.io.homekit.library.characteristic;

import java.util.Set;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Third Party Camera Active Characteristic.
 * This characteristic represents whether a third-party camera is active or not.
 *
 * @see <a href=\"https://developer.apple.com/documentation/HomeKit\">HAP Specification</a>
 * @author Karel Goderis - Initial Contribution
 */
@HomekitCharacteristicType(type = "0000021C-0000-1000-8000-0026BB765291", name = "Third Party Camera Active", tag = "thirdPartyCameraActive", acceptedItemTypes = {
        "Switch" })
@NonNullByDefault
public class HomekitThirdPartyCameraActiveCharacteristic extends HomekitIntegerCharacteristic {
    /**
     * Enum representing the possible states for the third-party camera.
     */
    public enum CameraActiveState {
        OFF(0),
        ON(1);

        private final int code;

        CameraActiveState(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static CameraActiveState fromCode(int code) {
            for (CameraActiveState s : values()) {
                if (s.code == code)
                    return s;
            }
            return OFF;
        }
    }

    public HomekitThirdPartyCameraActiveCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, 0, 1, "");
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(false).withEvents(true)
                .withDescription("Third Party Camera Active");
    }

    public HomekitThirdPartyCameraActiveCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
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
