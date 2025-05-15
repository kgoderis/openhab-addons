package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitStringCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Model Characteristic.
 * This characteristic represents the model name of the accessory.
 * It is a read-only string value that identifies the model of the device.
 *
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(
    type = "00000021-0000-1000-8000-0026BB765291",
    name = "Model",
    tag = "model",
    acceptedItemTypes = {"String", "Text"}
)
@NonNullByDefault
public class HomekitModelCharacteristic extends HomekitStringCharacteristic {

    /**
     * Creates a new Model characteristic.
     * This characteristic provides the model name of the accessory as a read-only value.
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param instanceId The instance ID for this characteristic
     */
    public HomekitModelCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(false)
                .withDescription("Model");
    }

    /**
     * Creates a new Model characteristic from a JSON value.
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param value The JSON value to initialize the characteristic with
     */
    public HomekitModelCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }
}
