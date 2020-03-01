package org.openhab.io.homekit.library.characteristic;

import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.ManagedService;
import org.openhab.io.homekit.internal.characteristic.IntegerCharacteristic;

public class CurrentPositionCharacteristic extends IntegerCharacteristic {

    public CurrentPositionCharacteristic(HomekitCommunicationManager manager, ManagedService service, long instanceId) {
        super(manager, service, instanceId, false, true, true, "Current position of an accessory", 0, 100, "%");
    }

    public static String getType() {
        return "0000006D-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }

}
