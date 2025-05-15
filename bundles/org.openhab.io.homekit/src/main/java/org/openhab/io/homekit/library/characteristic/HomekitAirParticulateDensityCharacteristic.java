package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitFloatCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Air Particulate Density Characteristic.
 * This characteristic represents the density of particulate matter in the air.
 * The value ranges from 0 to 1000 micrograms per cubic meter.
 *
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "00000064-0000-1000-8000-0026BB765291", name = "Air Particulate Density", tag = "airParticulateDensity", acceptedItemTypes = {"Number"})
@NonNullByDefault
public class HomekitAirParticulateDensityCharacteristic extends HomekitFloatCharacteristic {

    public HomekitAirParticulateDensityCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, 0.0, 1000.0, 1.0, "micrograms");
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(false).withEvents(true)
                .withDescription("Air Particulate Density");
    }

    public HomekitAirParticulateDensityCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Double value) {
        return value != null && value >= 0.0 && value <= 1000.0;
    }

    @Override
    public java.util.Set<Double> getAllowedValues() {
        return java.util.Collections.emptySet(); // No specific allowed values, just a range
    }
}
