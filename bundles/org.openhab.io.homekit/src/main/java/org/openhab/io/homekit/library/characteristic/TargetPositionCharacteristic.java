package org.openhab.io.homekit.library.characteristic;

import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.Service;
import org.openhab.io.homekit.internal.characteristic.IntegerCharacteristic;

public class TargetPositionCharacteristic extends IntegerCharacteristic {

    public TargetPositionCharacteristic(HomekitCommunicationManager manager, Service service, long instanceId) {
        super(manager, service, instanceId, true, true, true, "Target position of an accessory", 0, 100, "%");
    }

    public static String getType() {
        return "0000007C-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }

}
