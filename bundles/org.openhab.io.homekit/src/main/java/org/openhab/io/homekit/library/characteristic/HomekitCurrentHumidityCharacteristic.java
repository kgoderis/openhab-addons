package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitFloatCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Current Humidity Characteristic.
 * This characteristic represents the current relative humidity level.
 * The value is a percentage between 0 and 100.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/CurrentRelativeHumidity">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "00000010-0000-1000-8000-0026BB765291", name = "Current Relative Humidity", tag = "currentRelativeHumidity")
@NonNullByDefault
public class HomekitCurrentHumidityCharacteristic extends HomekitFloatCharacteristic {
    private static final double MIN_VALUE = 0.0;
    private static final double MAX_VALUE = 100.0;
    private static final double MIN_STEP = 1.0;

    public HomekitCurrentHumidityCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, MIN_VALUE, MAX_VALUE, MIN_STEP, "%");
        withInstanceId(instanceId)
            .withPairedWrite(false)
            .withPairedRead(true)
            .withEvents(true)
            .withDescription("Current Relative Humidity");
    }

    public HomekitCurrentHumidityCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Double value) {
        return value != null && value >= MIN_VALUE && value <= MAX_VALUE;
    }
} 