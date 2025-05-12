package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitBooleanCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * Obstruction Detected characteristic.
 * This characteristic represents whether an obstruction has been detected by the accessory.
 *
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 * @author Karel Goderis - Initial contribution
 */
@NonNullByDefault
@HomekitCharacteristicType(type = "00000024-0000-1000-8000-0026BB765291", name = "Obstruction Detected", tag = "obstructionDetected")
public class HomekitObstructionDetectedCharacteristic extends HomekitBooleanCharacteristic {

    public HomekitObstructionDetectedCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(true)
                .withDescription("Obstruction Detected");
    }

    public HomekitObstructionDetectedCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }
}
