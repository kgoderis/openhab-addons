package org.openhab.io.homekit.library.characteristic;

import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.Service;
import org.openhab.io.homekit.internal.characteristic.ReadOnlyStringCharacteristic;

public class VersionCharacteristic extends ReadOnlyStringCharacteristic {

    public VersionCharacteristic(HomekitCommunicationManager manager, Service service, long instanceId)
            throws Exception {
        super(manager, service, instanceId, "Version", "");
    }

    public static String getType() {
        return "00000037-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }

    public void setVersion(String version) {
        setReadOnlyValue(version);
    }
}
