package org.openhab.io.homekit.library.characteristic;

import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.ManagedService;
import org.openhab.io.homekit.internal.characteristic.ReadOnlyStringCharacteristic;

public class NameCharacteristic extends ReadOnlyStringCharacteristic {

    public NameCharacteristic(HomekitCommunicationManager manager, ManagedService service, long instanceId) {
        super(manager, service, instanceId, "Name",
                ((org.openhab.io.homekit.api.ManagedAccessory) service.getAccessory()).getLabel());
    }

    public static String getType() {
        return "00000023-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }
}
