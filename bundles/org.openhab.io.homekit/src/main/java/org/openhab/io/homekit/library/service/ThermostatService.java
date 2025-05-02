package org.openhab.io.homekit.library.service;

import java.util.Collection;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.io.homekit.api.factory.HomekitFactory;
import org.openhab.io.homekit.api.hap.Accessory;
import org.openhab.io.homekit.internal.events.HomekitEventManager;
import org.openhab.io.homekit.internal.service.GenericService;
import org.openhab.io.homekit.library.characteristic.CurrentHeatingCoolingStateCharacteristic;
import org.openhab.io.homekit.library.characteristic.CurrentTemperatureCharacteristic;
import org.openhab.io.homekit.library.characteristic.TargetHeatingCoolingStateCharacteristic;
import org.openhab.io.homekit.library.characteristic.TargetTemperatureCharacteristic;
import org.openhab.io.homekit.library.characteristic.TemperatureDisplayUnitsCharacteristic;

public class ThermostatService extends GenericService {
    private static final String TYPE = "0000004A-0000-1000-8000-0026BB765291";

    public ThermostatService(Accessory accessory, long instanceId, boolean extend, @NonNull String serviceName, HomekitEventManager eventManager, Collection<HomekitFactory> factories)
           {
        super(accessory, instanceId, extend, serviceName, TYPE, eventManager, factories);
    }

    public ThermostatService(Accessory accessory, JsonValue value, String name, HomekitEventManager eventManager, Collection<HomekitFactory> factories) {
        super(accessory, value, name, eventManager, factories);
    }

    @Override
    public void addCharacteristics() {
        super.addCharacteristics();
        addCharacteristic(
                new CurrentHeatingCoolingStateCharacteristic(this, ((Accessory) getAccessory()).getNextAvailableInstanceId(), eventManager));
        addCharacteristic(
                new TargetHeatingCoolingStateCharacteristic(this, ((Accessory) getAccessory()).getNextAvailableInstanceId(), eventManager));
        addCharacteristic(
                new CurrentTemperatureCharacteristic(this, ((Accessory) getAccessory()).getNextAvailableInstanceId(), eventManager));
        addCharacteristic(
                new TargetTemperatureCharacteristic(this, ((Accessory) getAccessory()).getNextAvailableInstanceId(), eventManager));
        addCharacteristic(
                new TemperatureDisplayUnitsCharacteristic(this, ((Accessory) getAccessory()).getNextAvailableInstanceId(), eventManager));
    }

    @Override
    public boolean isExtensible() {
        return false;
    }

    public static String getType() {
        return TYPE;
    }


    public static String getTag() {
        return ThermostatService.class.getSimpleName().replace("Service", "");
    }
}
