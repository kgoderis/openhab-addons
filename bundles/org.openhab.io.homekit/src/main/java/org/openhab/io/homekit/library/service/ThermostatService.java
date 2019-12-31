package org.openhab.io.homekit.library.service;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.Accessory;
import org.openhab.io.homekit.internal.service.AbstractService;
import org.openhab.io.homekit.library.characteristic.CurrentHeatingCoolingStateCharacteristic;
import org.openhab.io.homekit.library.characteristic.CurrentTemperatureCharacteristic;
import org.openhab.io.homekit.library.characteristic.TargetHeatingCoolingStateCharacteristic;
import org.openhab.io.homekit.library.characteristic.TargetTemperatureCharacteristic;
import org.openhab.io.homekit.library.characteristic.TemperatureDisplayUnitsCharacteristic;

public class ThermostatService extends AbstractService {

    public ThermostatService(HomekitCommunicationManager manager, Accessory accessory, long instanceId, boolean extend,
            @NonNull String serviceName) throws Exception {
        super(manager, accessory, instanceId, extend, serviceName);
    }

    @Override
    public void addCharacteristics() throws Exception {
        super.addCharacteristics();
        addCharacteristic(
                new CurrentHeatingCoolingStateCharacteristic(manager, this, this.getAccessory().getInstanceId()));
        addCharacteristic(
                new TargetHeatingCoolingStateCharacteristic(manager, this, this.getAccessory().getInstanceId()));
        addCharacteristic(new CurrentTemperatureCharacteristic(manager, this, this.getAccessory().getInstanceId()));
        addCharacteristic(new TargetTemperatureCharacteristic(manager, this, this.getAccessory().getInstanceId()));
        addCharacteristic(
                new TemperatureDisplayUnitsCharacteristic(manager, this, this.getAccessory().getInstanceId()));
    }

    @Override
    public boolean isExtensible() {
        return false;
    }

    public static String getType() {
        return "0000004A-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }
}
