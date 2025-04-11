package org.openhab.io.homekit.library.characteristic;

import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.internal.characteristic.IntegerCharacteristic;

public class CurrentVerticalTiltAngleCharacteristic extends IntegerCharacteristic {

    public CurrentVerticalTiltAngleCharacteristic(Service service, long instanceId) {
        super(service, instanceId, false, true, true, "The current angle of vertical slats", -90, 90, "arcdegrees");
    }

    public static String getType() {
        return "0000006E-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }

    public static String getTag() {
        return CurrentVerticalTiltAngleCharacteristic.class.getSimpleName().replace("Characteristic", "");
    }
}
