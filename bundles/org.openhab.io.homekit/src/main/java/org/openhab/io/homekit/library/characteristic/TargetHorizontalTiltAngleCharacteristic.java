package org.openhab.io.homekit.library.characteristic;

import org.openhab.io.homekit.api.Service;
import org.openhab.io.homekit.internal.characteristic.IntegerCharacteristic;

public class TargetHorizontalTiltAngleCharacteristic extends IntegerCharacteristic {

    protected TargetHorizontalTiltAngleCharacteristic(Service service, long instanceId) {
        super(service, instanceId, true, true, true, "Target horizontal tilt angle", -90, 90, "arcdegrees");
    }

    public static String getType() {
        return "0000007B-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }

}
