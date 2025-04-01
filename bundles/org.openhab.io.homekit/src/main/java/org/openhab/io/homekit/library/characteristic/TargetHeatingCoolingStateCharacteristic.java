package org.openhab.io.homekit.library.characteristic;

import org.openhab.io.homekit.api.Service;
import org.openhab.io.homekit.internal.characteristic.ByteCharacteristic;

public class TargetHeatingCoolingStateCharacteristic extends ByteCharacteristic {

    protected TargetHeatingCoolingStateCharacteristic(Service service, long instanceId) {
        super(service, instanceId, true, true, true, "Target heating cooling state", (byte) 0, (byte) 3);
    }

    public static String getType() {
        return "00000033-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }

}
