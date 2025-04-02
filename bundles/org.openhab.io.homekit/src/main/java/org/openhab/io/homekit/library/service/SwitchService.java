package org.openhab.io.homekit.library.service;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.io.homekit.api.hap.Accessory;
import org.openhab.io.homekit.internal.service.GenericService;
import org.openhab.io.homekit.library.characteristic.OnCharacteristic;

public class SwitchService extends GenericService {

    public SwitchService(Accessory accessory, long instanceId, boolean extend, @NonNull String serviceName) {
        super(accessory, instanceId, extend, serviceName);
    }

    @Override
    public void addCharacteristics() {
        super.addCharacteristics();
        addCharacteristic(
                new OnCharacteristic(this, getAccessory().getNewInstanceId()));
    }

    @Override
    public boolean isExtensible() {
        return false;
    }

    public static String getType() {
        return "00000049-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }
}
