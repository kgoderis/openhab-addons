package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

@HomekitCharacteristicType(type = "0000001D-0000-1000-8000-0026BB765291", name = "Lock Current State", tag = "lockCurrentState")
@NonNullByDefault
public class HomekitLockCurrentStateCharacteristic extends HomekitEnumCharacteristic {

    public enum LockCurrentState {
        UNSECURED(0),
        SECURED(1),
        JAMMED(2),
        UNKNOWN(3);

        private final int code;

        LockCurrentState(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static LockCurrentState fromCode(int code) {
            for (LockCurrentState state : values()) {
                if (state.code == code) {
                    return state;
                }
            }
            return UNKNOWN;
        }
    }

    public HomekitLockCurrentStateCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, LockCurrentState.values().length);
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(true)
            .withDescription("Lock Current State");
    }

    public HomekitLockCurrentStateCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= LockCurrentState.UNSECURED.getCode() && 
               value <= LockCurrentState.UNKNOWN.getCode();
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(
            LockCurrentState.UNSECURED.getCode(),
            LockCurrentState.SECURED.getCode(),
            LockCurrentState.JAMMED.getCode(),
            LockCurrentState.UNKNOWN.getCode()
        );
    }
} 