package org.openhab.io.homekit.library.characteristic;

import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.ManagedService;
import org.openhab.io.homekit.internal.characteristic.EnumCharacteristic;

public class StatusFaultCharacteristic extends EnumCharacteristic {

    public StatusFaultCharacteristic(HomekitCommunicationManager manager, ManagedService service, long instanceId) {
        super(manager, service, instanceId, false, true, true, "State of an accessory which has a fault", 1);
    }

    public static String getType() {
        return "00000077-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }

}
