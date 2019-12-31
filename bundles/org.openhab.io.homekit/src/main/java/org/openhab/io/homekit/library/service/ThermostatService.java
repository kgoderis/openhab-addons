package org.openhab.io.homekit.library.service;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.ManagedAccessory;
import org.openhab.io.homekit.internal.service.AbstractManagedService;
import org.openhab.io.homekit.library.characteristic.CurrentHeatingCoolingStateCharacteristic;
import org.openhab.io.homekit.library.characteristic.CurrentTemperatureCharacteristic;
import org.openhab.io.homekit.library.characteristic.TargetHeatingCoolingStateCharacteristic;
import org.openhab.io.homekit.library.characteristic.TargetTemperatureCharacteristic;
import org.openhab.io.homekit.library.characteristic.TemperatureDisplayUnitsCharacteristic;

public class ThermostatService extends AbstractManagedService {

    public ThermostatService(HomekitCommunicationManager manager, ManagedAccessory accessory, long instanceId,
            boolean extend, @NonNull String serviceName) throws Exception {
        super(manager, accessory, instanceId, extend, serviceName);
    }

    @Override
    public void addCharacteristics() {
        super.addCharacteristics();
        addCharacteristic(new CurrentHeatingCoolingStateCharacteristic(getManager(), this,
                ((ManagedAccessory) getAccessory()).getInstanceId()));
        addCharacteristic(new TargetHeatingCoolingStateCharacteristic(getManager(), this,
                ((ManagedAccessory) getAccessory()).getInstanceId()));
        addCharacteristic(new CurrentTemperatureCharacteristic(getManager(), this,
                ((ManagedAccessory) getAccessory()).getInstanceId()));
        addCharacteristic(new TargetTemperatureCharacteristic(getManager(), this,
                ((ManagedAccessory) getAccessory()).getInstanceId()));
        addCharacteristic(new TemperatureDisplayUnitsCharacteristic(getManager(), this,
                ((ManagedAccessory) getAccessory()).getInstanceId()));
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
