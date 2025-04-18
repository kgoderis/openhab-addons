package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.internal.characteristic.ReadOnlyStringCharacteristic;

public class FirmwareRevisionCharacteristic extends ReadOnlyStringCharacteristic {

    public FirmwareRevisionCharacteristic(Service service, long instanceId) {
        super(service, instanceId, "Firmware revision of the accessory");
    }

    public FirmwareRevisionCharacteristic(Service service, JsonValue value) {
        super(service, value);
    }

    public static String getType() {
        return "00000052-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }

    public static String getTag() {
        return FirmwareRevisionCharacteristic.class.getSimpleName().replace("Characteristic", "");
    }
}
