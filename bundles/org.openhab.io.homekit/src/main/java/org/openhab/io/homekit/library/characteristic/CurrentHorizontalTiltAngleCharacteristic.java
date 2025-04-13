package org.openhab.io.homekit.library.characteristic;

import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.internal.characteristic.IntegerCharacteristic;
import javax.json.JsonValue;

public class CurrentHorizontalTiltAngleCharacteristic extends IntegerCharacteristic {

    public CurrentHorizontalTiltAngleCharacteristic(Service service, long instanceId) {
        super(service, instanceId, false, true, true, "Current horizontal tilt angle", -90, 90, "arcdegrees");
    }

    public CurrentHorizontalTiltAngleCharacteristic(Service service, JsonValue value) {
        super(service, value);
    }

    public static String getType() {
        return "0000006C-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }

    public static String getTag() {
        return CurrentHorizontalTiltAngleCharacteristic.class.getSimpleName().replace("Characteristic", "");
    }
}
