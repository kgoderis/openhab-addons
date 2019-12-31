package org.openhab.io.homekit.library.service;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.Accessory;
import org.openhab.io.homekit.library.characteristic.ColorTemperatureCharacteristic;
import org.openhab.io.homekit.library.characteristic.HueCharacteristic;
import org.openhab.io.homekit.library.characteristic.SaturationCharacteristic;

public class ColorLightBulbService extends LightBulbService {

    public ColorLightBulbService(HomekitCommunicationManager manager, Accessory accessory, long instanceId,
            boolean extend, @NonNull String serviceName) throws Exception {
        super(manager, accessory, instanceId, extend, serviceName);
    }

    @Override
    public void addCharacteristics() throws Exception {
        super.addCharacteristics();
        addCharacteristic(new HueCharacteristic(manager, this, this.getAccessory().getInstanceId()));
        addCharacteristic(new SaturationCharacteristic(manager, this, this.getAccessory().getInstanceId()));
        addCharacteristic(new ColorTemperatureCharacteristic(manager, this, this.getAccessory().getInstanceId()));
    }
}
