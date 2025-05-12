package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitFloatCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Current Ambient Light Level Characteristic.
 * This characteristic represents the ambient light level in lux.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/CurrentAmbientLightLevel">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "0000006B-0000-1000-8000-0026BB765291", name = "Current Ambient Light Level", tag = "currentAmbientLightLevel")
@NonNullByDefault
public class HomekitCurrentAmbientLightLevelCharacteristic extends HomekitFloatCharacteristic {
    public HomekitCurrentAmbientLightLevelCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 0.0001, 100000.0, 0.0001, "lux");
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(true)
            .withDescription("Current Ambient Light Level");
    }
    public HomekitCurrentAmbientLightLevelCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }
    @Override
    public boolean isAllowedValue(Double value) {
        return value != null && value >= 0.0001 && value <= 100000.0;
    }
    @Override
    public java.util.Set<Double> getAllowedValues() {
        return java.util.Collections.emptySet();
    }
} 