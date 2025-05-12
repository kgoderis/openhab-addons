package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

@HomekitCharacteristicType(type = "000000B0-0000-1000-8000-0026BB765291", name = "Active", tag = "active")
@NonNullByDefault
public class HomekitActiveCharacteristic extends HomekitEnumCharacteristic {

    public enum Active {
        INACTIVE(0),
        ACTIVE(1);
        private final int value;
        Active(int value) { this.value = value; }
        public int getValue() { return value; }
        public static Active fromValue(int value) {
            for (Active a : values()) {
                if (a.value == value) return a;
            }
            throw new IllegalArgumentException("Invalid Active value: " + value);
        }
    }

    public HomekitActiveCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 2);
        withInstanceId(instanceId).withPairedWrite(true).withPairedRead(true).withEvents(true)
            .withDescription("Active");
    }

    public HomekitActiveCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        if (value == null) return false;
        for (Active a : Active.values()) {
            if (a.getValue() == value) return true;
        }
        return false;
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        java.util.Set<Integer> allowed = new java.util.HashSet<>();
        for (Active a : Active.values()) {
            allowed.add(a.getValue());
        }
        return allowed;
    }
} 