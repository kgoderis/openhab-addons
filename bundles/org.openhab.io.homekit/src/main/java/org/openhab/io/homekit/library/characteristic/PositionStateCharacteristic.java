package org.openhab.io.homekit.library.characteristic;

import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.internal.characteristic.EnumCharacteristic;

public class PositionStateCharacteristic extends EnumCharacteristic {

    public PositionStateCharacteristic(Service service, long instanceId) {
        super(service, instanceId, false, true, true, "Position state", 2);
    }

    public static String getType() {
        return "00000072-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }
}
