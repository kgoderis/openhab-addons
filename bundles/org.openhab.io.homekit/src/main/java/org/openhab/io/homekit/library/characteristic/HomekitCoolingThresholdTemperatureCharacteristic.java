package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitFloatCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Cooling Threshold Temperature Characteristic.
 * This characteristic is used to set the cooling threshold temperature for a thermostat (in Celsius).
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/CoolingThresholdTemperature">HomeKit Documentation</a>
 */
@NonNullByDefault
@HomekitCharacteristicType(type = "0000000D-0000-1000-8000-0026BB765291", name = "Cooling Threshold Temperature", tag = "coolingThresholdTemperature")
public class HomekitCoolingThresholdTemperatureCharacteristic extends HomekitFloatCharacteristic {


    public HomekitCoolingThresholdTemperatureCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 10.0, 35.0, 0.1, "celsius");
        withInstanceId(instanceId)
            .withPairedWrite(true)
            .withPairedRead(true)
            .withEvents(true)
            .withDescription("Cooling Threshold Temperature");
    }

    public HomekitCoolingThresholdTemperatureCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Double value) {
        return value != null && value >= 10.0 && value <= 35.0;
    }
} 