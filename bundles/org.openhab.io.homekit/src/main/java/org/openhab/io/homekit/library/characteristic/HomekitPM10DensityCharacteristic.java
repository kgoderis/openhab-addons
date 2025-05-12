package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitFloatCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit PM10 Density Characteristic.
 * This characteristic represents the PM10 density in micrograms per cubic meter.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/PM10Density">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "000000C7-0000-1000-8000-0026BB765291", name = "PM10 Density", tag = "pm10Density")
@NonNullByDefault
public class HomekitPM10DensityCharacteristic extends HomekitFloatCharacteristic {
    public HomekitPM10DensityCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 0.0, 1000.0, 1.0, "micrograms/m3");
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(true)
            .withDescription("PM10 Density");
    }
    public HomekitPM10DensityCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }
    @Override
    public boolean isAllowedValue(Double value) {
        return value != null && value >= 0.0 && value <= 1000.0;
    }
    @Override
    public java.util.Set<Double> getAllowedValues() {
        return java.util.Collections.emptySet();
    }
} 