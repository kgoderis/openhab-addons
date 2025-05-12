package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitFloatCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Heating Threshold Temperature Characteristic.
 * @see <a href="https://developers.homebridge.io/#/characteristic/HeatingThresholdTemperature">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "00000012-0000-1000-8000-0026BB765291", name = "Heating Threshold Temperature", tag = "heatingThresholdTemperature")
@NonNullByDefault
public class HomekitHeatingThresholdTemperatureCharacteristic extends HomekitFloatCharacteristic {

    public HomekitHeatingThresholdTemperatureCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 0.0, 25.0, 0.1, "°C");
        withInstanceId(instanceId)
            .withPairedWrite(true)
            .withPairedRead(true)
            .withEvents(true)
            .withDescription("Heating Threshold Temperature");
    }

    public HomekitHeatingThresholdTemperatureCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Double value) {
        return value != null && value >= 0.0 && value <= 25.0;
    }

    @Override
    public java.util.Set<Double> getAllowedValues() {
        return java.util.Collections.emptySet();
    }

    @Override
    public Double toValue(State state) {
        DecimalType convertedState = state.as(DecimalType.class);
        if (convertedState == null) {
            return 0.0;
        }
        return convertedState.doubleValue();
    }

    @Override
    public State toState(Double value) {
        return new DecimalType(value);
    }
} 