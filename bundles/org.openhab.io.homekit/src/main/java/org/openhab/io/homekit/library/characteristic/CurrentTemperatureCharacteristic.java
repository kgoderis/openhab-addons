package org.openhab.io.homekit.library.characteristic;

import org.openhab.io.homekit.api.Service;
import org.openhab.io.homekit.internal.characteristic.FloatCharacteristic;

public class CurrentTemperatureCharacteristic extends FloatCharacteristic {

    protected CurrentTemperatureCharacteristic(Service service, long instanceId) {
        super(service, instanceId, false, true, true, "Current temperature of the environment in Celsius", 0, 100, 0.1, "celcius");
    }

    public static String getType() {
        return "00000011-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }
}
