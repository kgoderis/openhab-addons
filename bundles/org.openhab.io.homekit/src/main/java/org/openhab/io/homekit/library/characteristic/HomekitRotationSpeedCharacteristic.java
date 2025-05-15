package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitFloatCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Rotation Speed Characteristic.
 * This characteristic represents the rotation speed of a device, expressed as a percentage (0-100).
 *
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "00000029-0000-1000-8000-0026BB765291", name = "Rotation Speed", tag = "rotationSpeed", acceptedItemTypes = {"Number"})
@NonNullByDefault
public class HomekitRotationSpeedCharacteristic extends HomekitFloatCharacteristic {

    public HomekitRotationSpeedCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, 0.0, 100.0, 1.0, "percentage");
        withInstanceId(instanceId).withPairedWrite(true).withPairedRead(true).withEvents(true)
                .withDescription("Rotation Speed");
    }

    public HomekitRotationSpeedCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Double value) {
        return value != null && value >= 0.0 && value <= 100.0;
    }

    @Override
    public java.util.Set<Double> getAllowedValues() {
        return java.util.Collections.emptySet(); // No specific allowed values, just a range
    }
}
