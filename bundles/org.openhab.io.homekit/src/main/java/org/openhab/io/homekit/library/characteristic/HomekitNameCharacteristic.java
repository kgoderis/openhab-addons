package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitReadOnlyStringCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Name Characteristic.
 * This characteristic represents the name of a device.
 * It is a read-only string value that identifies the device in the Home app.
 *
 * @author Karel Goderis - Initial Contribution
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "00000023-0000-1000-8000-0026BB765291", name = "Name", tag = "name", acceptedItemTypes = {"String", "Text"})
@NonNullByDefault
public class HomekitNameCharacteristic extends HomekitReadOnlyStringCharacteristic {
    public HomekitNameCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(false).withEvents(false)
                .withDescription("Name");
    }

    /**
     * Creates a new Name characteristic from a JSON value.
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param value The JSON value to initialize the characteristic with
     */
    public HomekitNameCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }
}
