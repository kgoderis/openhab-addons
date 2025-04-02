package org.openhab.io.homekit.library.service;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.io.homekit.api.Accessory;
import org.openhab.io.homekit.internal.service.GenericService;
import org.openhab.io.homekit.library.characteristic.CurrentHeatingCoolingStateCharacteristic;
import org.openhab.io.homekit.library.characteristic.CurrentTemperatureCharacteristic;
import org.openhab.io.homekit.library.characteristic.TargetHeatingCoolingStateCharacteristic;
import org.openhab.io.homekit.library.characteristic.TargetTemperatureCharacteristic;
import org.openhab.io.homekit.library.characteristic.TemperatureDisplayUnitsCharacteristic;

public class ThermostatService extends GenericService {

    public ThermostatService(Accessory accessory, long instanceId, boolean extend, @NonNull String serviceName) throws Exception {
        super(accessory, instanceId, extend, serviceName);
    }

    @Override
    public void addCharacteristics() {
        super.addCharacteristics();
        addCharacteristic(new CurrentHeatingCoolingStateCharacteristic(this, getAccessory().getNewInstanceId()));
        addCharacteristic(new TargetHeatingCoolingStateCharacteristic(this, getAccessory().getNewInstanceId()));
        addCharacteristic(new CurrentTemperatureCharacteristic(this, getAccessory().getNewInstanceId()));
        addCharacteristic(new TargetTemperatureCharacteristic(this, getAccessory().getNewInstanceId()));
        addCharacteristic(new TemperatureDisplayUnitsCharacteristic(this, getAccessory().getNewInstanceId()));
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
