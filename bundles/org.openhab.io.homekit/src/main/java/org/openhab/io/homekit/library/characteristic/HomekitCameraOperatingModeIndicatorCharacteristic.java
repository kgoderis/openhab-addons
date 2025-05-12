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
 * See the official HomeKit documentation for details.
 */
@HomekitCharacteristicType(type = "0000021D-0000-1000-8000-0026BB765291", name = "Camera Operating Mode Indicator", tag = "cameraOperatingModeIndicator")
@NonNullByDefault
public class HomekitCameraOperatingModeIndicatorCharacteristic extends HomekitBooleanCharacteristic {
    public HomekitCameraOperatingModeIndicatorCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId)
            .withPairedRead(true)
            .withPairedWrite(true)
            .withEvents(true)
            .withTimedWrite(true)
            .withDescription("Camera Operating Mode Indicator");
    }

    public HomekitCameraOperatingModeIndicatorCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }
} 