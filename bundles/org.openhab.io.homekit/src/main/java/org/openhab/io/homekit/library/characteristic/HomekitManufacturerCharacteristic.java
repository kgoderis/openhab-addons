package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitStringCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Manufacturer Characteristic.
 * This characteristic represents the manufacturer name of the accessory.
 *
 * @see <a href=\"https://developer.apple.com/documentation/HomeKit\">HAP Specification</a>
 * @author Karel Goderis - Initial Contribution
 */
@HomekitCharacteristicType(type = "00000020-0000-1000-8000-0026BB765291", name = "Manufacturer", tag = "manufacturer", acceptedItemTypes = {"String", "Text"})
@NonNullByDefault
public class HomekitManufacturerCharacteristic extends HomekitStringCharacteristic {

    public HomekitManufacturerCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(false)
                .withDescription("Manufacturer");
    }

    public HomekitManufacturerCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }
}
