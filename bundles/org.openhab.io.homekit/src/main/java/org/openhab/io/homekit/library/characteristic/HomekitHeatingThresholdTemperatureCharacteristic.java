package org.openhab.io.homekit.library.characteristic;

import java.util.Set;

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
 * <p>
 * This characteristic represents the heating threshold temperature for a thermostat or similar device, expressed in
 * degrees Celsius. The value is a floating-point number and is used to determine when heating should be activated.
 * <p>
 * See the HomeKit Accessory Protocol (HAP) specification for details: https://developer.apple.com/documentation/HomeKit
 *
 * @author Karel Goderis
 */
@HomekitCharacteristicType(type = "00000012-0000-1000-8000-0026BB765291", name = "Heating Threshold Temperature", tag = "heatingThresholdTemperature", acceptedItemTypes = {"Number"})
@NonNullByDefault
public class HomekitHeatingThresholdTemperatureCharacteristic extends HomekitFloatCharacteristic {

    /**
     * Constructs a new Heating Threshold Temperature characteristic.
     *
     * @param service the HomeKit service this characteristic belongs to
     * @param eventManager the event manager for handling HomeKit events
     * @param instanceId the instance ID for this characteristic
     */
    public HomekitHeatingThresholdTemperatureCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, 0.0, 25.0, 0.1, "celcius");
        withInstanceId(instanceId).withPairedWrite(true).withPairedRead(true).withEvents(true)
                .withDescription("Heating Threshold Temperature");
    }

    /**
     * Constructs a new Heating Threshold Temperature characteristic from a JSON value.
     *
     * @param service the HomeKit service this characteristic belongs to
     * @param eventManager the event manager for handling HomeKit events
     * @param value the JSON value to initialize the characteristic with
     */
    public HomekitHeatingThresholdTemperatureCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    /**
     * Checks if the given value is an allowed heating threshold temperature.
     *
     * @param value the value to check
     * @return true if the value is allowed, false otherwise
     */
    @Override
    public boolean isAllowedValue(Double value) {
        return value != null && value >= 0.0 && value <= 25.0;
    }

    /**
     * Returns the set of allowed heating threshold temperature values (empty set for continuous range).
     *
     * @return the set of allowed values
     */
    @Override
    public Set<Double> getAllowedValues() {
        return Set.of();
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
