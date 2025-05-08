package org.openhab.io.homekit.library.service;

import java.util.Collection;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.io.homekit.api.factory.HomekitFactory;
import org.openhab.io.homekit.api.hap.HomekitAccessory;
import org.openhab.io.homekit.internal.events.HomekitEventManager;
import org.openhab.io.homekit.internal.service.HomekitGenericService;
import org.openhab.io.homekit.library.characteristic.HomekitBrightnessCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitHueCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitOnCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitSaturationCharacteristic;

public class HomekitColorLightBulbService extends HomekitGenericService {
    private static final String TYPE = "00000043-0000-1000-8000-0026BB765291";

    public HomekitColorLightBulbService(HomekitAccessory accessory, long instanceId, boolean extend, @NonNull String serviceName, HomekitEventManager eventManager, Collection<HomekitFactory> factories)
            {
        super(accessory, instanceId, extend, serviceName, TYPE, eventManager, factories);
    }

    public HomekitColorLightBulbService(HomekitAccessory accessory, JsonValue value, String name, HomekitEventManager eventManager, Collection<HomekitFactory> factories) {
        super(accessory, value, name, eventManager, factories);
    }

    @Override
    public void addCharacteristics() {
        super.addCharacteristics();
        addCharacteristic(
                new HomekitOnCharacteristic(this, ((HomekitAccessory) getAccessory()).getNextAvailableInstanceId(), eventManager));
        addCharacteristic(new HomekitBrightnessCharacteristic(this, ((HomekitAccessory) getAccessory()).getNextAvailableInstanceId(),
                eventManager));
        addCharacteristic(
                new HomekitHueCharacteristic(this, ((HomekitAccessory) getAccessory()).getNextAvailableInstanceId(), eventManager));
        addCharacteristic(new HomekitSaturationCharacteristic(this, ((HomekitAccessory) getAccessory()).getNextAvailableInstanceId(),
                eventManager));
    }

    @Override
    public boolean isExtensible() {
        return false;
    }

    public static String getType() {
        return TYPE;
    }

    public static String getTag() {
        return HomekitColorLightBulbService.class.getSimpleName().replace("HomekitService", "");
    }
}
