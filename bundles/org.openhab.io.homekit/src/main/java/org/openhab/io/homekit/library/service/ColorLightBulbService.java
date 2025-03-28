package org.openhab.io.homekit.library.service;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.ManagedAccessory;
import org.openhab.io.homekit.library.characteristic.ColorTemperatureCharacteristic;
import org.openhab.io.homekit.library.characteristic.HueCharacteristic;
import org.openhab.io.homekit.library.characteristic.SaturationCharacteristic;

public class ColorLightBulbService extends LightBulbService {

    public ColorLightBulbService(HomekitCommunicationManager manager, ManagedAccessory accessory, long instanceId,
            boolean extend, @NonNull String serviceName) throws Exception {
        super(manager, accessory, instanceId, extend, serviceName);
    }

    @Override
    public void addCharacteristics() {
        super.addCharacteristics();
        addCharacteristic(
                new HueCharacteristic(getManager(), this, ((ManagedAccessory) getAccessory()).getInstanceId()));
        addCharacteristic(
                new SaturationCharacteristic(getManager(), this, ((ManagedAccessory) getAccessory()).getInstanceId()));
        addCharacteristic(new ColorTemperatureCharacteristic(getManager(), this,
                ((ManagedAccessory) getAccessory()).getInstanceId()));
    }
}
