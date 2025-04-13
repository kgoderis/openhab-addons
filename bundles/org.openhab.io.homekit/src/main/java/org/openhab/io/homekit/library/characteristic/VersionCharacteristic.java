package org.openhab.io.homekit.library.characteristic;

import org.openhab.core.OpenHAB;
import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.internal.characteristic.ShortReadOnlyStringCharacteristic;
import javax.json.JsonValue;

public class VersionCharacteristic extends ShortReadOnlyStringCharacteristic {

    public VersionCharacteristic(Service service, long instanceId) {
        super(service, instanceId, "1.0.0");
    }

    public VersionCharacteristic(Service service, JsonValue value) {
        super(service, value);
    }

    public static String getType() {
        return "00000037-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }

    @Override
    public String getValue() {
        return OpenHAB.getVersion();
    }

    public void setVersion(String version) {
        setReadOnlyValue(version);
    }

    public static String getTag() {
        return VersionCharacteristic.class.getSimpleName().replace("Characteristic", "");
    }
}
