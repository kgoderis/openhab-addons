package org.openhab.io.homekit.library.characteristic;

import org.openhab.io.homekit.api.Service;
import org.openhab.io.homekit.internal.characteristic.ReadOnlyStringCharacteristic;

public class ModelCharacteristic extends ReadOnlyStringCharacteristic {

    protected ModelCharacteristic(Service service, long instanceId) {
        super(service, instanceId, "Model of the accessory");
    }

    public static String getType() {
        return "00000021-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }
}
