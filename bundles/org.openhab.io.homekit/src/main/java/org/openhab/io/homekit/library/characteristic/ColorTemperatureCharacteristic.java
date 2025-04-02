package org.openhab.io.homekit.library.characteristic;

import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.internal.characteristic.LongCharacteristic;

public class ColorTemperatureCharacteristic extends LongCharacteristic {

    public ColorTemperatureCharacteristic(Service service, long instanceId) {
        super(service, instanceId, true, true, true, "Color temperature", 50L, 400L, 1L);
    }

    public static String getType() {
        return "000000CE-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }
}
