package org.openhab.io.homekit.library.characteristic;

import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.Service;
import org.openhab.io.homekit.internal.characteristic.IntegerCharacteristic;

public class TargetVerticalTiltAngleCharacteristic extends IntegerCharacteristic {

    public TargetVerticalTiltAngleCharacteristic(HomekitCommunicationManager manager, Service service,
            long instanceId) {
        super(manager, service, instanceId, true, true, true, "The target angle of vertical slats", -90, 90,
                "arcdegrees");
    }

    public static String getType() {
        return "0000006E-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }

}
