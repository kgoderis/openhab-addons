package org.openhab.io.homekit.library.service;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.Accessory;
import org.openhab.io.homekit.internal.service.AbstractService;
import org.openhab.io.homekit.library.characteristic.BrightnessCharacteristic;
import org.openhab.io.homekit.library.characteristic.ColorTemperatureCharacteristic;
import org.openhab.io.homekit.library.characteristic.HueCharacteristic;
import org.openhab.io.homekit.library.characteristic.NameCharacteristic;
import org.openhab.io.homekit.library.characteristic.OnCharacteristic;
import org.openhab.io.homekit.library.characteristic.SaturationCharacteristic;

public class LightBulbService extends AbstractService {

    public LightBulbService(HomekitCommunicationManager manager, Accessory accessory, long instanceId, boolean extend,
            @NonNull String serviceName) throws Exception {
        super(manager, accessory, instanceId, extend, serviceName);
    }

    @Override
    public void addCharacteristics() throws Exception {
        super.addCharacteristics();
        addCharacteristic(new OnCharacteristic(manager, this, this.getAccessory().getInstanceId()));
        addCharacteristic(new BrightnessCharacteristic(manager, this, this.getAccessory().getInstanceId()));
        addCharacteristic(new HueCharacteristic(manager, this, this.getAccessory().getInstanceId()));
        addCharacteristic(new NameCharacteristic(manager, this, this.getAccessory().getInstanceId()));
        addCharacteristic(new SaturationCharacteristic(manager, this, this.getAccessory().getInstanceId()));
        addCharacteristic(new ColorTemperatureCharacteristic(manager, this, this.getAccessory().getInstanceId()));
    }

    @Override
    public boolean isExtensible() {
        return false;
    }

    public static String getType() {
        return "00000043-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }
}
