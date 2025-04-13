package org.openhab.io.homekit.library.characteristic;

import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.internal.characteristic.IntegerCharacteristic;
import javax.json.JsonValue;

public class CurrentPositionCharacteristic extends IntegerCharacteristic {

    public CurrentPositionCharacteristic(Service service, long instanceId) {
        super(service, instanceId, false, true, true, "Current position", 0, 100, "percent");
    }

    public CurrentPositionCharacteristic(Service service, JsonValue value) {
        super(service, value);
    }

    public static String getType() {
        return "0000006D-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }

    public static String getTag() {
        return CurrentPositionCharacteristic.class.getSimpleName().replace("Characteristic", "");
    }
}
