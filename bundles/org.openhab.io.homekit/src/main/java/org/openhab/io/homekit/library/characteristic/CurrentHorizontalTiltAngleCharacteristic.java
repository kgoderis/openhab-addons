package org.openhab.io.homekit.library.characteristic;

import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.ManagedService;
import org.openhab.io.homekit.internal.characteristic.IntegerCharacteristic;

public class CurrentHorizontalTiltAngleCharacteristic extends IntegerCharacteristic {

    public CurrentHorizontalTiltAngleCharacteristic(HomekitCommunicationManager manager, ManagedService service,
            long instanceId) {
        super(manager, service, instanceId, false, true, true, "The current angle of horizontal slats", -90, 90,
                "arcdegrees");
    }

    public static String getType() {
        return "0000006C-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }

}
