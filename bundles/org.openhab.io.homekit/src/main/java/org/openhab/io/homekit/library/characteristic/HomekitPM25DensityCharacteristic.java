package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitFloatCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit PM2.5 Density Characteristic.
 * This characteristic represents the PM2.5 density in micrograms per cubic meter.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/PM2_5Density">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "000000C6-0000-1000-8000-0026BB765291", name = "PM2.5 Density", tag = "pm25Density")
@NonNullByDefault
public class HomekitPM25DensityCharacteristic extends HomekitFloatCharacteristic {
    public HomekitPM25DensityCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 0.0, 1000.0, 1.0, "micrograms/m3");
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(true)
            .withDescription("PM2.5 Density");
    }
    public HomekitPM25DensityCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
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