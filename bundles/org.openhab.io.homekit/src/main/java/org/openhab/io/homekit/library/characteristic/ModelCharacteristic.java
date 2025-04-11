package org.openhab.io.homekit.library.characteristic;

import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.internal.characteristic.ReadOnlyStringCharacteristic;

public class ModelCharacteristic extends ReadOnlyStringCharacteristic {

    public ModelCharacteristic(Service service, long instanceId) {
        super(service, instanceId, "Model of the accessory");
    }

    public static String getType() {
        return "00000021-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }

    public static String getTag() {
        return ModelCharacteristic.class.getSimpleName().replace("Characteristic", "");
    }
}
