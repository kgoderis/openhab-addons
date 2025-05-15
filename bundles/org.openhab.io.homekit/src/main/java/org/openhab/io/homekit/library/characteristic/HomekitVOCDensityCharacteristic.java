package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitFloatCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit VOC Density Characteristic.
 * This characteristic represents the density of volatile organic compounds (VOC) in the air.
 *
 * @see <a href=\"https://developer.apple.com/documentation/HomeKit\">HAP Specification</a>
 * @author Karel Goderis - Initial Contribution
 */
@HomekitCharacteristicType(type = "000000C8-0000-1000-8000-0026BB765291", name = "VOC Density", tag = "vocDensity")
@NonNullByDefault
public class HomekitVOCDensityCharacteristic extends HomekitFloatCharacteristic {

    public HomekitVOCDensityCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 0.0, 1000.0, 1.0, "micrograms/m3");
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(true)
                .withDescription("VOC Density");
    }

    public HomekitVOCDensityCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Double value) {
        return value != null && value >= 0.0 && value <= 1000.0;
    }
}
