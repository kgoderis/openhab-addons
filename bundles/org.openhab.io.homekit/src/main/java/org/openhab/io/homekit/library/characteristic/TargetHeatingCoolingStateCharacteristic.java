package org.openhab.io.homekit.library.characteristic;

import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.Service;
import org.openhab.io.homekit.internal.characteristic.ByteCharacteristic;

public class TargetHeatingCoolingStateCharacteristic extends ByteCharacteristic {

    public TargetHeatingCoolingStateCharacteristic(HomekitCommunicationManager manager, Service service,
            long instanceId) {
        super(manager, service, instanceId, true, true, true, "The current mode of a thermostat", (byte) 0, (byte) 3);
    }

    public static String getType() {
        return "00000033-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }
}
