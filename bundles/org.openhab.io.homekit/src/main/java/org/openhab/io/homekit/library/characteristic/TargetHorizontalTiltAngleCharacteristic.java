package org.openhab.io.homekit.library.characteristic;

import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.ManagedService;
import org.openhab.io.homekit.internal.characteristic.IntegerCharacteristic;

public class TargetHorizontalTiltAngleCharacteristic extends IntegerCharacteristic {

    public TargetHorizontalTiltAngleCharacteristic(HomekitCommunicationManager manager, ManagedService service,
            long instanceId) {
        super(manager, service, instanceId, true, true, true, "The target angle of horizontal slats", -90, 90,
                "arcdegrees");
    }

    public static String getType() {
        return "0000007B-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }

}
