package org.openhab.io.homekit.library.characteristic;

import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.internal.characteristic.ByteCharacteristic;
import javax.json.JsonValue;

public class TemperatureDisplayUnitsCharacteristic extends ByteCharacteristic {

    public TemperatureDisplayUnitsCharacteristic(Service service, long instanceId) {
        super(service, instanceId, true, true, true, "Temperature display units", (byte) 0, (byte) 1);
    }

    public TemperatureDisplayUnitsCharacteristic(Service service, JsonValue value) {
        super(service, value);
    }

    public static String getType() {
        return "00000036-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }

    public static String getTag() {
        return TemperatureDisplayUnitsCharacteristic.class.getSimpleName().replace("Characteristic", "");
    }
}
