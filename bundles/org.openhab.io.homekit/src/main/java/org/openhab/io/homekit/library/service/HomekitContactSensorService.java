package org.openhab.io.homekit.library.service;

import java.util.Collection;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.io.homekit.api.factory.HomekitFactory;
import org.openhab.io.homekit.api.hap.HomekitAccessory;
import org.openhab.io.homekit.internal.events.HomekitEventManager;
import org.openhab.io.homekit.internal.service.HomekitBaseService;
import org.openhab.io.homekit.library.characteristic.HomekitContactSensorStateCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitStatusActiveCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitStatusFaultCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitStatusLowBatteryCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitStatusTamperedCharacteristic;

public class HomekitContactSensorService extends HomekitBaseService {
    private static final String TYPE = "00000080-0000-1000-8000-0026BB765291";

    public HomekitContactSensorService(HomekitAccessory accessory, long instanceId, boolean extend, @NonNull String serviceName, HomekitEventManager eventManager, Collection<HomekitFactory> factories)
            {
        super(accessory, instanceId, extend, serviceName, TYPE, eventManager, factories);
    }

    public HomekitContactSensorService(HomekitAccessory accessory, JsonValue value, String name, HomekitEventManager eventManager, Collection<HomekitFactory> factories) {
        super(accessory, value, name, eventManager, factories);
    }

    @Override
    public void addCharacteristics() {
        super.addCharacteristics();
        addCharacteristic(new HomekitContactSensorStateCharacteristic(this,
                ((HomekitAccessory) getAccessory()).getNextAvailableInstanceId(), eventManager));
        addCharacteristic(new HomekitStatusActiveCharacteristic(this,
                ((HomekitAccessory) getAccessory()).getNextAvailableInstanceId(), eventManager));
        addCharacteristic(new HomekitStatusFaultCharacteristic(this, ((HomekitAccessory) getAccessory()).getNextAvailableInstanceId(),
                eventManager));
        addCharacteristic(new HomekitStatusTamperedCharacteristic(this,
                ((HomekitAccessory) getAccessory()).getNextAvailableInstanceId(), eventManager));
        addCharacteristic(new HomekitStatusLowBatteryCharacteristic(this,
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
        return HomekitContactSensorService.class.getSimpleName().replace("HomekitService", "");
    }
}
