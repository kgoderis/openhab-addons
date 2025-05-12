package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * Lock Physical Controls Characteristic.
 */
@HomekitCharacteristicType(type = "000000A7-0000-1000-8000-0026BB765291", name = "Lock Physical Controls", tag = "lockPhysicalControls")
@NonNullByDefault
public class HomekitLockPhysicalControlsCharacteristic extends HomekitIntegerCharacteristic {

    public enum LockPhysicalControls {
        DISABLED(0),
        ENABLED(1);
        private final int code;
        LockPhysicalControls(int code) { this.code = code; }
        public int getCode() { return code; }
        public static LockPhysicalControls fromCode(int code) {
            for (LockPhysicalControls s : values()) {
                if (s.code == code) return s;
            }
            return DISABLED;
        }
    }

    public HomekitLockPhysicalControlsCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 0, 1, "");
        withInstanceId(instanceId)
            .withPairedWrite(true)
            .withPairedRead(true)
            .withEvents(true)
            .withDescription("Lock Physical Controls");
    }

    public HomekitLockPhysicalControlsCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && (value == 0 || value == 1);
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        java.util.Set<Integer> set = new java.util.HashSet<>();
        for (LockPhysicalControls s : LockPhysicalControls.values()) {
            set.add(s.getCode());
        }
        return set;
    }
} 