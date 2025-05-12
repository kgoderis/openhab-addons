package org.openhab.io.homekit.library.service;

import javax.json.JsonValue;

import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.service.HomekitServiceType;
import org.openhab.io.homekit.core.service.AbstractHomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

@HomekitServiceType(type = "000000B9-0000-1000-8000-0026BB765291", name = "Slat", tag = "Slat")
public class HomekitSlatService extends AbstractHomekitService {

    public HomekitSlatService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory) {
        super(accessory, eventManager, characteristicFactory);
    }

    public HomekitSlatService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory, JsonValue value) {
        super(accessory, eventManager, characteristicFactory, value);
    }

    @Override
    public void addCharacteristics() {
        super.addCharacteristics();
        addCharacteristic(new HomekitCurrentSlatStateCharacteristic(this, getAccessory().getNextAvailableInstanceId(),
                eventManager));
        addCharacteristic(
                new HomekitSlatTypeCharacteristic(this, getAccessory().getNextAvailableInstanceId(), eventManager));
        addCharacteristic(new HomekitCurrentTiltAngleCharacteristic(this, getAccessory().getNextAvailableInstanceId(),
                eventManager));
        addCharacteristic(new HomekitTargetTiltAngleCharacteristic(this, getAccessory().getNextAvailableInstanceId(),
                eventManager));
    }

    @Override
    public boolean isExtensible() {
        return false;
    }
}
