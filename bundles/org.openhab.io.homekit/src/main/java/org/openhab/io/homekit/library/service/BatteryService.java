package org.openhab.io.homekit.library.service;

import java.util.Collection;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.io.homekit.api.factory.HomekitFactory;
import org.openhab.io.homekit.api.hap.Accessory;
import org.openhab.io.homekit.internal.events.HomekitEventManager;
import org.openhab.io.homekit.internal.service.GenericService;
import org.openhab.io.homekit.library.characteristic.StatusLowBatteryCharacteristic;

public class BatteryService extends GenericService {
    private static final String TYPE = "00000096-0000-1000-8000-0026BB765291";

    public BatteryService(Accessory accessory, long instanceId, boolean extend, @NonNull String serviceName,  HomekitEventManager eventManager, Collection<HomekitFactory> factories)
             {
        super(accessory, instanceId, extend, serviceName, TYPE, eventManager, factories);
    }

    public BatteryService(Accessory accessory, JsonValue value, String name, HomekitEventManager eventManager, Collection<HomekitFactory> factories) {
        super(accessory, value, name, eventManager, factories);
    }

    @Override
    public void addCharacteristics() {
        super.addCharacteristics();
        addCharacteristic(
                new StatusLowBatteryCharacteristic(this, ((Accessory) getAccessory()).getNextAvailableInstanceId(), eventManager));
    }

    @Override
    public boolean isExtensible() {
        return false;
    }

    public static String getType() {
        return TYPE;
    }

    public static String getTag() {
        return BatteryService.class.getSimpleName().replace("Service", "");
    }
}
