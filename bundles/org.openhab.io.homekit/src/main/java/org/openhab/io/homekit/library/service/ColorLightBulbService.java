package org.openhab.io.homekit.library.service;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.Accessory;
import org.openhab.io.homekit.library.characteristic.ColorTemperatureCharacteristic;
import org.openhab.io.homekit.library.characteristic.HueCharacteristic;
import org.openhab.io.homekit.library.characteristic.SaturationCharacteristic;

public class ColorLightBulbService extends LightBulbService {

    public ColorLightBulbService( Accessory accessory, long instanceId,
            boolean extend, @NonNull String serviceName) throws Exception {
        super( accessory, instanceId, extend, serviceName);
    }

    @Override
    public void addCharacteristics() {
        super.addCharacteristics();
        addCharacteristic(
                new HueCharacteristic( this, ((Accessory) getAccessory()).getNewInstanceId()));
        addCharacteristic(
                new SaturationCharacteristic  (this, ((Accessory) getAccessory()).getNewInstanceId()));
        addCharacteristic(new ColorTemperatureCharacteristic ( this,
                ((Accessory) getAccessory()).getNewInstanceId()));
    }
}
