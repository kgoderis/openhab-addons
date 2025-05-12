package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Current Visibility State Characteristic.
 * This characteristic represents the current visibility state (e.g., for a window covering).
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/CurrentVisibilityState">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "00000135-0000-1000-8000-0026BB765291", name = "Current Visibility State", tag = "currentVisibilityState")
@NonNullByDefault
public class HomekitCurrentVisibilityStateCharacteristic extends HomekitEnumCharacteristic {
    public enum CurrentVisibilityState {
        SHOWN(0),
        HIDDEN(1);
        private final int code;
        CurrentVisibilityState(int code) { this.code = code; }
        public int getCode() { return code; }
        public static CurrentVisibilityState fromCode(int code) {
            for (CurrentVisibilityState s : values()) {
                if (s.code == code) return s;
            }
            return SHOWN;
        }
    }
    public HomekitCurrentVisibilityStateCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, CurrentVisibilityState.values().length);
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(true)
            .withDescription("Current Visibility State");
    }
    public HomekitCurrentVisibilityStateCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }
    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && (value == CurrentVisibilityState.SHOWN.getCode() || value == CurrentVisibilityState.HIDDEN.getCode());
    }
    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(CurrentVisibilityState.SHOWN.getCode(), CurrentVisibilityState.HIDDEN.getCode());
    }
} 