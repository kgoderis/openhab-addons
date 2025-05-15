package org.openhab.io.homekit.core.accessory;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.accessory.HomekitAccessoryType;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.factory.HomekitServiceFactory;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

@NonNullByDefault
@HomekitAccessoryType(name = "Generic Accessory", type = "1110001-0000-1000-8000-0026BB765291", tag = "generic")
public class HomekitGenericAccessory extends AbstractHomekitAccessory {

    public HomekitGenericAccessory(HomekitEventManager eventManager, HomekitServiceFactory serviceFactory,
            HomekitCharacteristicFactory characteristicFactory) {
        super(eventManager, serviceFactory, characteristicFactory);
    }

    public HomekitGenericAccessory(HomekitEventManager eventManager, HomekitServiceFactory serviceFactory,
            HomekitCharacteristicFactory characteristicFactory, JsonValue value) {
        super(eventManager, serviceFactory, characteristicFactory, value);
    }
}
