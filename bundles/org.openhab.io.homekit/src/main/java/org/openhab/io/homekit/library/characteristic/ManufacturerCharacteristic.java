package org.openhab.io.homekit.library.characteristic;

import org.openhab.io.homekit.api.Service;
import org.openhab.io.homekit.internal.characteristic.ReadOnlyStringCharacteristic;

public class ManufacturerCharacteristic extends ReadOnlyStringCharacteristic {

    public ManufacturerCharacteristic(Service service, long instanceId) {
        super(service, instanceId, "Manufacturer of the accessory");
    }

    public static String getType() {
        return "00000020-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }
}
