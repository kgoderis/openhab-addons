package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitStringCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Software Revision Characteristic.
 * This characteristic represents the software revision of the accessory.
 *
 * @see <a href=\"https://developer.apple.com/documentation/HomeKit\">HAP Specification</a>
 * @author Karel Goderis - Initial Contribution
 */
@HomekitCharacteristicType(type = "00000054-0000-1000-8000-0026BB765291", name = "Software Revision", tag = "softwareRevision")
@NonNullByDefault
public class HomekitSoftwareRevisionCharacteristic extends HomekitStringCharacteristic {

    public HomekitSoftwareRevisionCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId)
            .withPairedWrite(false)
            .withPairedRead(true)
            .withEvents(false)
            .withDescription("Software Revision");
    }

    public HomekitSoftwareRevisionCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }
} 