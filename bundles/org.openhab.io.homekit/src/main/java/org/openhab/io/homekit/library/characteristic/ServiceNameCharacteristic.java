package org.openhab.io.homekit.library.characteristic;

import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.internal.characteristic.ReadOnlyStringCharacteristic;

public class ServiceNameCharacteristic extends ReadOnlyStringCharacteristic {

    public ServiceNameCharacteristic(Service service, long instanceId) {
        super(service, instanceId, "Name of the service");
    }

    @Override
    public String getInstanceType() {
        return "00000023-0000-1000-8000-0026BB765291";
    }
}
