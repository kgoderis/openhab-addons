package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitFloatCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Carbon Dioxide Peak Level Characteristic.
 * This characteristic represents the peak level of carbon dioxide detected by the accessory.
 *
 * @see <a href=\"https://developer.apple.com/documentation/HomeKit\">HAP Specification</a>
 * @author Karel Goderis - Initial Contribution
 */
@HomekitCharacteristicType(type = "00000094-0000-1000-8000-0026BB765291", name = "Carbon Dioxide Peak Level", tag = "carbonDioxidePeakLevel")
@NonNullByDefault
public class HomekitCarbonDioxidePeakLevelCharacteristic extends HomekitFloatCharacteristic {

    public HomekitCarbonDioxidePeakLevelCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 0.0, 10000.0, 1.0, "");
        withInstanceId(instanceId)
            .withPairedWrite(false)
            .withPairedRead(true)
            .withEvents(true)
            .withDescription("Carbon Dioxide Peak Level");
    }

    public HomekitCarbonDioxidePeakLevelCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Double value) {
        return value != null && value >= 0.0 && value <= 10000.0;
    }

    @Override
    public java.util.Set<Double> getAllowedValues() {
        return java.util.Collections.emptySet();
    }
} 