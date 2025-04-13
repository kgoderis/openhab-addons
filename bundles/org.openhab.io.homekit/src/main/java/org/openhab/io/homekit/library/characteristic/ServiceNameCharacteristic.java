package org.openhab.io.homekit.library.characteristic;

import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.internal.characteristic.ReadOnlyStringCharacteristic;
import javax.json.JsonValue;

public class ServiceNameCharacteristic extends ReadOnlyStringCharacteristic {

    public ServiceNameCharacteristic(Service service, long instanceId) {
        super(service, instanceId, "Name of the service");
    }

    public ServiceNameCharacteristic(Service service, JsonValue value) {
        super(service, value);
    }

    @Override
    public String getInstanceType() {
        return getType();
    }

    public static String getType() {
        return "00000023-0000-1000-8000-0026BB765291";
    }

    public static String getTag() {
        return ServiceNameCharacteristic.class.getSimpleName().replace("Characteristic", "");
    }
}
