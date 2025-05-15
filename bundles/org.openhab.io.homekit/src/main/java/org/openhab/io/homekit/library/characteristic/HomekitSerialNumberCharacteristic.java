package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitReadOnlyStringCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Serial Number Characteristic.
 * This characteristic represents the serial number of a device.
 * It is a read-only string value that uniquely identifies the device.
 * The serial number should be a unique identifier for the device instance.
 *
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "00000030-0000-1000-8000-0026BB765291", name = "Serial Number", tag = "serialNumber", acceptedItemTypes = {"String", "Text"})
@NonNullByDefault
public class HomekitSerialNumberCharacteristic extends HomekitReadOnlyStringCharacteristic {
    public HomekitSerialNumberCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(false).withEvents(false)
                .withDescription("Serial Number");
    }

    public HomekitSerialNumberCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }
}
