package org.openhab.io.homekit.library.service;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.io.homekit.api.hap.Accessory;
import org.openhab.io.homekit.internal.service.GenericService;
import org.openhab.io.homekit.library.characteristic.StatusLowBatteryCharacteristic;

public class BatteryService extends GenericService {

    public BatteryService(Accessory accessory, long instanceId,
            boolean extend, @NonNull String serviceName) throws Exception {
        super( accessory, instanceId, extend, serviceName);
    }

    @Override
    public void addCharacteristics() {
        super.addCharacteristics();
        addCharacteristic(new StatusLowBatteryCharacteristic( this,
                ((Accessory) getAccessory()).getNewInstanceId()));
    }

    @Override
    public boolean isExtensible() {
        return false;
    }

    public static String getType() {
        return "00000096-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }
}
