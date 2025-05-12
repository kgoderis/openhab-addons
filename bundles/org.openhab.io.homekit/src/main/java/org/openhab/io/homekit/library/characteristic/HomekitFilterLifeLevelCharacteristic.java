package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitFloatCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Filter Life Level Characteristic.
 * This characteristic represents the remaining life of a filter as a percentage (0-100).
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/FilterLifeLevel">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "000000AB-0000-1000-8000-0026BB765291", name = "Filter Life Level", tag = "filterLifeLevel")
@NonNullByDefault
public class HomekitFilterLifeLevelCharacteristic extends HomekitFloatCharacteristic {
    public HomekitFilterLifeLevelCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 0.0, 100.0, 1.0, "%");
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(true)
            .withDescription("Filter Life Level");
    }
    public HomekitFilterLifeLevelCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }
    @Override
    public boolean isAllowedValue(Double value) {
        return value != null && value >= 0.0 && value <= 100.0;
    }
    @Override
    public java.util.Set<Double> getAllowedValues() {
        return java.util.Collections.emptySet();
    }
} 