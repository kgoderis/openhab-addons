package org.openhab.io.homekit.library.characteristic;

import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.ManagedService;
import org.openhab.io.homekit.internal.characteristic.EnumCharacteristic;

public class PositionStateCharacteristic extends EnumCharacteristic {

    public PositionStateCharacteristic(HomekitCommunicationManager manager, ManagedService service, long instanceId) {
        super(manager, service, instanceId, false, true, true, "State of the position of an accessory", 2);
    }

    public static String getType() {
        return "00000072-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }

}
