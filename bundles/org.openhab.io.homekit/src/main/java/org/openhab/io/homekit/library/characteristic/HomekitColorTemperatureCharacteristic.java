package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitFloatCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * Color Temperature characteristic.
 * This characteristic represents the color temperature in mireds.
 *
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 * @author Karel Goderis - Initial contribution
 */
@HomekitCharacteristicType(type = "000000CE-0000-1000-8000-0026BB765291", name = "Color Temperature", tag = "colorTemperature", acceptedItemTypes = {
        "Number", "Dimmer" })
@NonNullByDefault
public class HomekitColorTemperatureCharacteristic extends HomekitFloatCharacteristic {

    public HomekitColorTemperatureCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, 140.0, 500.0, 1.0, "mired");
        withInstanceId(instanceId).withPairedWrite(true).withPairedRead(true).withEvents(true)
                .withDescription("Color Temperature");
    }

    public HomekitColorTemperatureCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Double value) {
        return value != null && value >= 140.0 && value <= 500.0;
    }

    @Override
    public java.util.Set<Double> getAllowedValues() {
        return java.util.Collections.emptySet();
    }
}
