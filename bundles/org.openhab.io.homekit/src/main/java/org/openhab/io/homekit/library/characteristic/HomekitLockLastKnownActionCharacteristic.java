package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Lock Last Known Action Characteristic.
 * @see <a href="https://developers.homebridge.io/#/characteristic/LockLastKnownAction">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "0000001C-0000-1000-8000-0026BB765291", name = "Lock Last Known Action", tag = "lockLastKnownAction")
@NonNullByDefault
public class HomekitLockLastKnownActionCharacteristic extends HomekitIntegerCharacteristic {

    public enum LockLastKnownAction {
        SECURED_PHYSICALLY_INTERIOR(0),
        UNSECURED_PHYSICALLY_INTERIOR(1),
        SECURED_PHYSICALLY_EXTERIOR(2),
        UNSECURED_PHYSICALLY_EXTERIOR(3),
        SECURED_BY_KEYPAD(4),
        UNSECURED_BY_KEYPAD(5),
        SECURED_REMOTELY(6),
        UNSECURED_REMOTELY(7),
        SECURED_BY_AUTO_SECURE_TIMEOUT(8),
        SECURED_PHYSICALLY(9),
        UNSECURED_PHYSICALLY(10);
        private final int code;
        LockLastKnownAction(int code) { this.code = code; }
        public int getCode() { return code; }
        public static LockLastKnownAction fromCode(int code) {
            for (LockLastKnownAction a : values()) {
                if (a.code == code) return a;
            }
            return null;
        }
    }

    public HomekitLockLastKnownActionCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 0, 10, "");
        withInstanceId(instanceId)
            .withPairedWrite(false)
            .withPairedRead(true)
            .withEvents(true)
            .withDescription("Lock Last Known Action");
    }

    public HomekitLockLastKnownActionCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= 0 && value <= 10;
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        java.util.Set<Integer> set = new java.util.HashSet<>();
        for (LockLastKnownAction a : LockLastKnownAction.values()) {
            set.add(a.getCode());
        }
        return set;
    }
} 