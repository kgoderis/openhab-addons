package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.internal.characteristic.IntegerCharacteristic;

public class TargetPositionCharacteristic extends IntegerCharacteristic {

    public TargetPositionCharacteristic(Service service, long instanceId) {
        super(service, instanceId, true, true, true, "Target position", 0, 100, "%");
    }

    public TargetPositionCharacteristic(Service service, JsonValue value) {
        super(service, value);
    }

    public static String getType() {
        return "0000007C-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }

    public static String getTag() {
        return TargetPositionCharacteristic.class.getSimpleName().replace("Characteristic", "");
    }
}
