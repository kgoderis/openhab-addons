package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitFloatCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Battery Level Characteristic.
 * This characteristic represents the battery level as a percentage (0-100).
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/BatteryLevel">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "00000068-0000-1000-8000-0026BB765291", name = "Battery Level", tag = "batteryLevel")
@NonNullByDefault
public class HomekitBatteryLevelCharacteristic extends HomekitFloatCharacteristic {
    public HomekitBatteryLevelCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 0.0, 100.0, 1.0, "%");
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(true)
            .withDescription("Battery Level");
    }
    public HomekitBatteryLevelCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
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