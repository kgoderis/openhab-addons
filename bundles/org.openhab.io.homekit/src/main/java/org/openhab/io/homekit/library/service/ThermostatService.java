package org.openhab.io.homekit.library.service;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.io.homekit.api.hap.Accessory;
import org.openhab.io.homekit.internal.service.GenericService;
import org.openhab.io.homekit.library.characteristic.CurrentHeatingCoolingStateCharacteristic;
import org.openhab.io.homekit.library.characteristic.CurrentTemperatureCharacteristic;
import org.openhab.io.homekit.library.characteristic.TargetHeatingCoolingStateCharacteristic;
import org.openhab.io.homekit.library.characteristic.TargetTemperatureCharacteristic;
import org.openhab.io.homekit.library.characteristic.TemperatureDisplayUnitsCharacteristic;

public class ThermostatService extends GenericService {

    public ThermostatService(Accessory accessory, long instanceId, boolean extend, @NonNull String serviceName)
            throws Exception {
        super(accessory, instanceId, extend, serviceName);
    }

    public ThermostatService(Accessory accessory, JsonValue value, String name) {
        super(accessory, value, name);
    }

    @Override
    public void addCharacteristics() {
        super.addCharacteristics();
        addCharacteristic(
                new CurrentHeatingCoolingStateCharacteristic(this, getAccessory().getNextAvailableInstanceId()));
        addCharacteristic(
                new TargetHeatingCoolingStateCharacteristic(this, getAccessory().getNextAvailableInstanceId()));
        addCharacteristic(new CurrentTemperatureCharacteristic(this, getAccessory().getNextAvailableInstanceId()));
        addCharacteristic(new TargetTemperatureCharacteristic(this, getAccessory().getNextAvailableInstanceId()));
        addCharacteristic(new TemperatureDisplayUnitsCharacteristic(this, getAccessory().getNextAvailableInstanceId()));
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

    public static String getTag() {
        return ThermostatService.class.getSimpleName().replace("Service", "");
    }
}
