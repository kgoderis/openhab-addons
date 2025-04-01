package org.openhab.io.homekit.library.characteristic;

import org.openhab.core.OpenHAB;
import org.openhab.io.homekit.api.Service;
import org.openhab.io.homekit.internal.characteristic.ShortReadOnlyStringCharacteristic;

public class VersionCharacteristic extends ShortReadOnlyStringCharacteristic {

    protected VersionCharacteristic(Service service, long instanceId) {
        super(service, instanceId, "Version");
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
}
