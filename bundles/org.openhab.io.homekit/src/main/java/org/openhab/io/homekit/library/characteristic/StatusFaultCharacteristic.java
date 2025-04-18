package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.internal.characteristic.EnumCharacteristic;

public class StatusFaultCharacteristic extends EnumCharacteristic {

    public StatusFaultCharacteristic(Service service, long instanceId) {
        super(service, instanceId, false, true, true, "Status fault", 1);
    }

    public StatusFaultCharacteristic(Service service, JsonValue value) {
        super(service, value);
    }

    public static String getType() {
        return "00000077-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }

    public static String getTag() {
        return StatusFaultCharacteristic.class.getSimpleName().replace("Characteristic", "");
    }
}
