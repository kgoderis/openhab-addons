package org.openhab.io.homekit.library.characteristic;

import org.openhab.io.homekit.api.Service;
import org.openhab.io.homekit.internal.characteristic.ByteCharacteristic;

public class TemperatureDisplayUnitsCharacteristic extends ByteCharacteristic {

    protected TemperatureDisplayUnitsCharacteristic(Service service, long instanceId) {
        super(service, instanceId, true, true, true, "Temperature display units", (byte) 0, (byte) 1);
    }

    public static String getType() {
        return "00000036-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }

}
