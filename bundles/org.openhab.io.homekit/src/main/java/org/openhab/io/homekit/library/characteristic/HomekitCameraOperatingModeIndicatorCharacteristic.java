package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitBooleanCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Camera Operating Mode Indicator Characteristic.
 * This characteristic represents whether the camera operating mode indicator is enabled or disabled.
 *
 * @see <a href=\"https://developer.apple.com/documentation/HomeKit\">HAP Specification</a>
 * @author Karel Goderis - Initial Contribution
 */
@HomekitCharacteristicType(type = "0000021D-0000-1000-8000-0026BB765291", name = "Camera Operating Mode Indicator", tag = "cameraOperatingModeIndicator", acceptedItemTypes = {"Switch", "Contact"})
@NonNullByDefault
public class HomekitCameraOperatingModeIndicatorCharacteristic extends HomekitBooleanCharacteristic {
    public HomekitCameraOperatingModeIndicatorCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(true).withEvents(true).withTimedWrite(true)
                .withDescription("Camera Operating Mode Indicator");
    }

    public HomekitCameraOperatingModeIndicatorCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }
}
