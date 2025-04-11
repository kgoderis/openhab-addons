package org.openhab.io.homekit.library.service;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.io.homekit.api.hap.Accessory;
import org.openhab.io.homekit.internal.service.GenericService;
import org.openhab.io.homekit.library.characteristic.OnCharacteristic;
import org.openhab.io.homekit.library.characteristic.OutletInUseCharacteristic;

public class OutletService extends GenericService {

    public OutletService(Accessory accessory, long instanceId, boolean extend, @NonNull String serviceName)
            throws Exception {
        super(accessory, instanceId, extend, serviceName);
    }

    @Override
    public void addCharacteristics() {
        super.addCharacteristics();
        addCharacteristic(new OnCharacteristic(this, getAccessory().getNextAvailableInstanceId()));
        addCharacteristic(new OutletInUseCharacteristic(this, getAccessory().getNextAvailableInstanceId()));
    }

    @Override
    public boolean isExtensible() {
        return false;
    }

    public static String getType() {
        return "00000047-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }
}
