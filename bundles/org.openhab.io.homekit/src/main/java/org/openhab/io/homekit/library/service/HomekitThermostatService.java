package org.openhab.io.homekit.library.service;

import java.util.Collection;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.factory.HomekitFactory;
import org.openhab.io.homekit.core.service.HomekitBaseService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.library.characteristic.HomekitCurrentHeatingCoolingStateCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitCurrentTemperatureCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitTargetHeatingCoolingStateCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitTargetTemperatureCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitTemperatureDisplayUnitsCharacteristic;

public class HomekitThermostatService extends HomekitBaseService {
    private static final String TYPE = "0000004A-0000-1000-8000-0026BB765291";

    public HomekitThermostatService(HomekitAccessory accessory, long instanceId, boolean extend, @NonNull String serviceName, HomekitEventManager eventManager, Collection<HomekitFactory> factories)
           {
        super(accessory, instanceId, extend, serviceName, TYPE, eventManager, factories);
    }

    public HomekitThermostatService(HomekitAccessory accessory, JsonValue value, String name, HomekitEventManager eventManager, Collection<HomekitFactory> factories) {
        super(accessory, value, name, eventManager, factories);
    }

    @Override
    public void addCharacteristics() {
        super.addCharacteristics();
        addCharacteristic(new HomekitCurrentHeatingCoolingStateCharacteristic(this,
                ((HomekitAccessory) getAccessory()).getNextAvailableInstanceId(), eventManager));
        addCharacteristic(new HomekitTargetHeatingCoolingStateCharacteristic(this,
                ((HomekitAccessory) getAccessory()).getNextAvailableInstanceId(), eventManager));
        addCharacteristic(new HomekitCurrentTemperatureCharacteristic(this,
                ((HomekitAccessory) getAccessory()).getNextAvailableInstanceId(), eventManager));
        addCharacteristic(new HomekitTargetTemperatureCharacteristic(this,
                ((HomekitAccessory) getAccessory()).getNextAvailableInstanceId(), eventManager));
        addCharacteristic(new HomekitTemperatureDisplayUnitsCharacteristic(this,
                ((HomekitAccessory) getAccessory()).getNextAvailableInstanceId(), eventManager));
    }

    @Override
    public boolean isExtensible() {
        return false;
    }

    public static String getType() {
        return TYPE;
    }

    public static String getTag() {
        return HomekitThermostatService.class.getSimpleName().replace("HomekitService", "");
    }
}
