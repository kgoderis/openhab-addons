package org.openhab.io.homekit.library.service;

import java.util.Collection;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.factory.HomekitFactory;
import org.openhab.io.homekit.core.service.HomekitBaseService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.library.characteristic.HomekitOnCharacteristic;

public class HomekitSwitchService extends HomekitBaseService {
    private static final String TYPE = "00000049-0000-1000-8000-0026BB765291";

    public HomekitSwitchService(HomekitAccessory accessory, long instanceId, boolean extend, @NonNull String serviceName, HomekitEventManager eventManager, Collection<HomekitFactory> factories)
            {
        super(accessory, instanceId, extend, serviceName, TYPE, eventManager, factories);
    }

    public HomekitSwitchService(HomekitAccessory accessory, JsonValue value, String name, HomekitEventManager eventManager, Collection<HomekitFactory> factories) {
        super(accessory, value, name, eventManager, factories);
    }

    @Override
    public void addCharacteristics() {
        super.addCharacteristics();
        addCharacteristic(
                new HomekitOnCharacteristic(this, ((HomekitAccessory) getAccessory()).getNextAvailableInstanceId(), eventManager));
    }

    @Override
    public boolean isExtensible() {
        return false;
    }

    public static String getType() {
        return TYPE;
    }

    public static String getTag() {
        return HomekitSwitchService.class.getSimpleName().replace("HomekitService", "");
    }
}
