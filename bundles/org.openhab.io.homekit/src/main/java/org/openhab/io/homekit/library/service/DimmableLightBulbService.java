package org.openhab.io.homekit.library.service;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.hap.Accessory;
import org.openhab.io.homekit.library.characteristic.BrightnessCharacteristic;

public class DimmableLightBulbService extends LightBulbService {

    public DimmableLightBulbService(Accessory accessory, long instanceId,
            boolean extend, @NonNull String serviceName) throws Exception {
        super( accessory, instanceId, extend, serviceName);
    }

    @Override
    public void addCharacteristics() {
        super.addCharacteristics();
        addCharacteristic(
                new BrightnessCharacteristic  (this, ((Accessory) getAccessory()).getNewInstanceId()));
    }
}
