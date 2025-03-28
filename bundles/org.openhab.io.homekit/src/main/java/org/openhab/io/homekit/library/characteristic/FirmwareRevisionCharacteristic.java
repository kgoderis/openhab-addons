package org.openhab.io.homekit.library.characteristic;

import org.openhab.core.OpenHAB;
import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.ManagedService;
import org.openhab.io.homekit.internal.characteristic.ReadOnlyStringCharacteristic;

public class FirmwareRevisionCharacteristic extends ReadOnlyStringCharacteristic {

    public FirmwareRevisionCharacteristic(HomekitCommunicationManager manager, ManagedService service,
            long instanceId) {
        super(manager, service, instanceId, "Firmware revision", OpenHAB.getVersion());
    }

    public static String getType() {
        return "00000052-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }
}
