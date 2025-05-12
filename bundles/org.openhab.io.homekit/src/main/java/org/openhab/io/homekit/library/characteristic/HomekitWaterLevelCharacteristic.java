package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitFloatCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Water Level Characteristic.
 * @see <a href="https://developers.homebridge.io/#/characteristic/WaterLevel">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "000000B5-0000-1000-8000-0026BB765291", name = "Water Level", tag = "waterLevel")
@NonNullByDefault
public class HomekitWaterLevelCharacteristic extends HomekitFloatCharacteristic {

    public HomekitWaterLevelCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 0.0, 100.0, 1.0, "%");
        withInstanceId(instanceId)
            .withPairedWrite(false)
            .withPairedRead(true)
            .withEvents(true)
            .withDescription("Water Level");
    }

    public HomekitWaterLevelCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Double value) {
        return value != null && value >= 0.0 && value <= 100.0;
    }
} 