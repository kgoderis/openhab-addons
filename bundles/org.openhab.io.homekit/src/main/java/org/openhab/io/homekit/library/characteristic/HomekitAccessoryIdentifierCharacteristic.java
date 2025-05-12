package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitStringCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

@HomekitCharacteristicType(type = "00000057-0000-1000-8000-0026BB765291", name = "Accessory Identifier", tag = "accessoryIdentifier")
@NonNullByDefault
public class HomekitAccessoryIdentifierCharacteristic extends HomekitStringCharacteristic {

    public HomekitAccessoryIdentifierCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(false)
            .withDescription("Accessory Identifier");
    }

    public HomekitAccessoryIdentifierCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }
} 