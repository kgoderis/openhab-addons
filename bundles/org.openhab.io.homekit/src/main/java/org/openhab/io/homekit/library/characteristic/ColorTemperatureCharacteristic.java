package org.openhab.io.homekit.library.characteristic;

import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.ManagedService;
import org.openhab.io.homekit.internal.characteristic.LongCharacteristic;

public class ColorTemperatureCharacteristic extends LongCharacteristic {

    public ColorTemperatureCharacteristic(HomekitCommunicationManager manager, ManagedService service, long instanceId) {
        super(manager, service, instanceId, true, true, true,
                "The color temperature which is represented in reciprocal megaKelvin (MK-1) or mirek scale", 50, 400,
                1);
    }

    public static String getType() {
        return "000000CE-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }

}
