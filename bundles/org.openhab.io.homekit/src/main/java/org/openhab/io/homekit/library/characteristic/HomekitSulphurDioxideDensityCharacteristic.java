package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitFloatCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Sulphur Dioxide Density Characteristic.
 * This characteristic represents the density of sulphur dioxide in the air, measured in micrograms per cubic meter.
 *
 * @see <a href=\"https://developer.apple.com/documentation/HomeKit\">HAP Specification</a>
 * @author Karel Goderis - Initial Contribution
 */
@HomekitCharacteristicType(type = "000000C5-0000-1000-8000-0026BB765291", name = "Sulphur Dioxide Density", tag = "sulphurDioxideDensity")
@NonNullByDefault
public class HomekitSulphurDioxideDensityCharacteristic extends HomekitFloatCharacteristic {

    public HomekitSulphurDioxideDensityCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 0.0, 1000.0, 1.0, "micrograms/m3");
        withInstanceId(instanceId)
            .withPairedWrite(false)
            .withPairedRead(true)
            .withEvents(true)
            .withDescription("Sulphur Dioxide Density");
    }

    public HomekitSulphurDioxideDensityCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    /**
     * Checks if the given value is a valid sulphur dioxide density.
     *
     * @param value the value to check
     * @return true if the value is within the allowed range, false otherwise
     */
    @Override
    public boolean isAllowedValue(Double value) {
        return value != null && value >= 0.0 && value <= 1000.0;
    }
} 