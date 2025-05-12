package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Target Visibility State Characteristic.
 * This characteristic represents the target visibility state of a device.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/TargetVisibilityState">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "00000134-0000-1000-8000-0026BB765291", name = "Target Visibility State", tag = "targetVisibilityState")
@NonNullByDefault
public class HomekitTargetVisibilityStateCharacteristic extends HomekitEnumCharacteristic {
    public enum TargetVisibilityState {
        SHOWN(0),
        HIDDEN(1);

        private final int code;

        TargetVisibilityState(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static TargetVisibilityState fromCode(int code) {
            for (TargetVisibilityState state : values()) {
                if (state.code == code) {
                    return state;
                }
            }
            return SHOWN;
        }
    }

    public HomekitTargetVisibilityStateCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, TargetVisibilityState.values().length);
        withInstanceId(instanceId)
            .withPairedRead(true)
            .withPairedWrite(true)
            .withEvents(true)
            .withDescription("Target Visibility State");
    }

    public HomekitTargetVisibilityStateCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && (value == TargetVisibilityState.SHOWN.getCode() || value == TargetVisibilityState.HIDDEN.getCode());
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(
            TargetVisibilityState.SHOWN.getCode(),
            TargetVisibilityState.HIDDEN.getCode()
        );
    }
} 