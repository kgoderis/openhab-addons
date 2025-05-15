package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitFloatCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Relative Humidity Humidifier Threshold Characteristic.
 * This characteristic represents the relative humidity threshold for a humidifier, as a percentage between 0 and 100.
 *
 * @see <a href=\"https://developer.apple.com/documentation/HomeKit\">HAP Specification</a>
 * @author Karel Goderis - Initial Contribution
 */
@NonNullByDefault
@HomekitCharacteristicType(type = "000000CA-0000-1000-8000-0026BB765291", name = "Relative Humidity Humidifier Threshold", tag = "relativeHumidityHumidifierThreshold", acceptedItemTypes = {
        "Number" })
public class HomekitRelativeHumidityHumidifierThresholdCharacteristic extends HomekitFloatCharacteristic {
    public HomekitRelativeHumidityHumidifierThresholdCharacteristic(HomekitService service,
            HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 0.0, 100.0, 1.0, "%");
        withInstanceId(instanceId).withPairedWrite(true).withPairedRead(true).withEvents(true)
                .withDescription("Relative Humidity Humidifier Threshold");
    }

    public HomekitRelativeHumidityHumidifierThresholdCharacteristic(HomekitService service,
            HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    /**
     * Checks if the given value is a valid relative humidity threshold for a humidifier.
     *
     * @param value the value to check
     * @return true if the value is within the allowed range, false otherwise
     */
    @Override
    public boolean isAllowedValue(Double value) {
        return value != null && value >= 0.0 && value <= 100.0;
    }
}
