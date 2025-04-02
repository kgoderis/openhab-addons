package org.openhab.io.homekit.library.characteristic;

import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.internal.characteristic.ReadOnlyStringCharacteristic;

public class NameCharacteristic extends ReadOnlyStringCharacteristic {

    public NameCharacteristic(Service service, long instanceId) {
        super(service, instanceId, "Name of the accessory");
    }

    public static String getType() {
        return "00000023-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }
}
