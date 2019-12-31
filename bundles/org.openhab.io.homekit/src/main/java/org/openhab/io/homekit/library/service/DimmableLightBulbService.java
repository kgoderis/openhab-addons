package org.openhab.io.homekit.library.service;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.Accessory;
import org.openhab.io.homekit.library.characteristic.BrightnessCharacteristic;

public class DimmableLightBulbService extends LightBulbService {

    public DimmableLightBulbService(HomekitCommunicationManager manager, Accessory accessory, long instanceId,
            boolean extend, @NonNull String serviceName) throws Exception {
        super(manager, accessory, instanceId, extend, serviceName);
    }

    @Override
    public void addCharacteristics() throws Exception {
        super.addCharacteristics();
        addCharacteristic(new BrightnessCharacteristic(manager, this, this.getAccessory().getInstanceId()));
    }
}
