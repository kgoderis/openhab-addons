package org.openhab.io.homekit.library.service;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.Accessory;
import org.openhab.io.homekit.internal.service.AbstractService;
import org.openhab.io.homekit.library.characteristic.StatusLowBatteryCharacteristic;

public class BatteryService extends AbstractService {

    public BatteryService(HomekitCommunicationManager manager, Accessory accessory, long instanceId, boolean extend,
            @NonNull String serviceName) throws Exception {
        super(manager, accessory, instanceId, extend, serviceName);
    }

    @Override
    public void addCharacteristics() throws Exception {
        super.addCharacteristics();
        addCharacteristic(new StatusLowBatteryCharacteristic(manager, this, this.getAccessory().getInstanceId()));
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
