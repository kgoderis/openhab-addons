package org.openhab.io.homekit.library.service;

import java.util.Collection;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.factory.HomekitFactory;
import org.openhab.io.homekit.core.service.HomekitBaseService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.library.characteristic.HomekitFirmwareRevisionCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitIdentifyCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitManufacturerCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitModelCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitNameCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitSerialNumberCharacteristic;

public class HomekitAccessoryInformationService extends HomekitBaseService {
    private static final String TYPE = "0000003E-0000-1000-8000-0026BB765291";

    public HomekitAccessoryInformationService(HomekitAccessory accessory, long instanceId, boolean extend, @NonNull String serviceName, HomekitEventManager eventManager, Collection<HomekitFactory> factories) {
        super(accessory, instanceId, extend, serviceName, TYPE, eventManager, factories);
    }

    public HomekitAccessoryInformationService(HomekitAccessory accessory, JsonValue value, String name, HomekitEventManager eventManager, Collection<HomekitFactory> factories) {
        super(accessory, value, name, eventManager, factories);
    }

    @Override
    public void addCharacteristics() {
        super.addCharacteristics();
        addCharacteristic(new HomekitIdentifyCharacteristic(this, ((HomekitAccessory) getAccessory()).getNextAvailableInstanceId(),
                eventManager));
        addCharacteristic(new HomekitManufacturerCharacteristic(this,
                ((HomekitAccessory) getAccessory()).getNextAvailableInstanceId(), eventManager));
        addCharacteristic(
                new HomekitModelCharacteristic(this, ((HomekitAccessory) getAccessory()).getNextAvailableInstanceId(), eventManager));
        addCharacteristic(
                new HomekitNameCharacteristic(this, ((HomekitAccessory) getAccessory()).getNextAvailableInstanceId(), eventManager));
        addCharacteristic(new HomekitSerialNumberCharacteristic(this,
                ((HomekitAccessory) getAccessory()).getNextAvailableInstanceId(), eventManager));
        addCharacteristic(new HomekitFirmwareRevisionCharacteristic(this,
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
        return HomekitAccessoryInformationService.class.getSimpleName().replace("HomekitService", "");
    }
}
