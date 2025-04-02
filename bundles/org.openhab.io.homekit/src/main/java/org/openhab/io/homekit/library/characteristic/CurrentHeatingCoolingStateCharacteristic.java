package org.openhab.io.homekit.library.characteristic;

import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.internal.characteristic.ByteCharacteristic;

public class CurrentHeatingCoolingStateCharacteristic extends ByteCharacteristic {

    public CurrentHeatingCoolingStateCharacteristic(Service service, long instanceId) {
        super(service, instanceId, false, true, true, "Current heating cooling state", (byte) 0, (byte) 3);
    }

    public static String getType() {
        return "0000000F-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }

}
