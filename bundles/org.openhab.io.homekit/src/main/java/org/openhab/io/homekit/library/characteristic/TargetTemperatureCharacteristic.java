package org.openhab.io.homekit.library.characteristic;

import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.internal.characteristic.FloatCharacteristic;

public class TargetTemperatureCharacteristic extends FloatCharacteristic {

    public TargetTemperatureCharacteristic(Service service, long instanceId) {
        super(service, instanceId, true, true, true, "Target temperature in Celsius", 10, 38, 0.1, "celcius");
    }

    public static String getType() {
        return "00000035-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }
}
