package org.openhab.io.homekit.library.characteristic;

import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.internal.characteristic.ReadOnlyStringCharacteristic;
import javax.json.JsonValue;

public class SerialNumberCharacteristic extends ReadOnlyStringCharacteristic {

    public SerialNumberCharacteristic(Service service, long instanceId) {
        super(service, instanceId, "Serial number of the accessory");
    }

    public SerialNumberCharacteristic(Service service, JsonValue value) {
        super(service, value);
    }

    public static String getType() {
        return "00000030-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }

    public static String getTag() {
        return SerialNumberCharacteristic.class.getSimpleName().replace("Characteristic", "");
    }
}
