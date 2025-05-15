package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitReadOnlyStringCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Firmware Revision Characteristic.
 * This characteristic represents the firmware revision of a device.
 * It is a read-only string value that indicates the version of firmware installed.
 *
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "00000052-0000-1000-8000-0026BB765291", name = "Firmware Revision", tag = "firmwareRevision", acceptedItemTypes = {"String", "Text"})
@NonNullByDefault
public class HomekitFirmwareRevisionCharacteristic extends HomekitReadOnlyStringCharacteristic {
    public HomekitFirmwareRevisionCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(false).withEvents(false)
                .withDescription("Firmware Revision");
    }

    public HomekitFirmwareRevisionCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }
}
