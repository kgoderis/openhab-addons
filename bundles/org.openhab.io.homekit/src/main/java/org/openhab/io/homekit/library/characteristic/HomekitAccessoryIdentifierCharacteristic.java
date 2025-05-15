package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitStringCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Accessory Identifier Characteristic.
 * This characteristic represents the unique identifier of an accessory.
 * It is a read-only string value that uniquely identifies the accessory in the Home app.
 *
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "00000057-0000-1000-8000-0026BB765291", name = "Accessory Identifier", tag = "accessoryIdentifier", acceptedItemTypes = {"String", "Text"})
@NonNullByDefault
public class HomekitAccessoryIdentifierCharacteristic extends HomekitStringCharacteristic {

    public HomekitAccessoryIdentifierCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(false).withEvents(false)
                .withDescription("Accessory Identifier");
    }

    public HomekitAccessoryIdentifierCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }
}
