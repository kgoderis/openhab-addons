package org.openhab.io.homekit.library.service;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.io.homekit.api.hap.Accessory;
import org.openhab.io.homekit.library.characteristic.ColorTemperatureCharacteristic;
import org.openhab.io.homekit.library.characteristic.HueCharacteristic;
import org.openhab.io.homekit.library.characteristic.SaturationCharacteristic;

public class ColorLightBulbService extends LightBulbService {

    public ColorLightBulbService(Accessory accessory, long instanceId, boolean extend, @NonNull String serviceName)
            throws Exception {
        super(accessory, instanceId, extend, serviceName);
    }

    @Override
    public void addCharacteristics() {
        super.addCharacteristics();
        addCharacteristic(new HueCharacteristic(this, ((Accessory) getAccessory()).getNextAvailableInstanceId()));
        addCharacteristic(
                new SaturationCharacteristic(this, ((Accessory) getAccessory()).getNextAvailableInstanceId()));
        addCharacteristic(
                new ColorTemperatureCharacteristic(this, ((Accessory) getAccessory()).getNextAvailableInstanceId()));
    }

    @Override
    public String getInstanceType() {
        return getType();
    }

    public static String getTag() {
        return ColorLightBulbService.class.getSimpleName().replace("Service", "");
    }
}
