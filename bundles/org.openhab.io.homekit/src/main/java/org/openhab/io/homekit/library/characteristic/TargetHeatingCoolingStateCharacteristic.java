package org.openhab.io.homekit.library.characteristic;

import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.internal.characteristic.ByteCharacteristic;
import javax.json.JsonValue;

public class TargetHeatingCoolingStateCharacteristic extends ByteCharacteristic {

    public TargetHeatingCoolingStateCharacteristic(Service service, long instanceId) {
        super(service, instanceId, true, true, true, "Target heating cooling state", (byte) 0, (byte) 3);
    }

    public TargetHeatingCoolingStateCharacteristic(Service service, JsonValue value) {
        super(service, value);
    }

    public static String getType() {
        return "00000033-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }

    public static String getTag() {
        return TargetHeatingCoolingStateCharacteristic.class.getSimpleName().replace("Characteristic", "");
    }
}
