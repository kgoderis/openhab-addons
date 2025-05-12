package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Target Tilt Angle Characteristic.
 * This characteristic represents the target tilt angle for a device (e.g., window covering).
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/TargetTiltAngle">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "000000C2-0000-1000-8000-0026BB765291", name = "Target Tilt Angle", tag = "targetTiltAngle")
@NonNullByDefault
public class HomekitTargetTiltAngleCharacteristic extends HomekitIntegerCharacteristic {
    public HomekitTargetTiltAngleCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, -90, 90, "°");
        withInstanceId(instanceId)
            .withPairedWrite(true)
            .withPairedRead(true)
            .withEvents(true)
            .withDescription("Target Tilt Angle");
    }
    public HomekitTargetTiltAngleCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }
    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= -90 && value <= 90;
    }
    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Collections.emptySet();
    }
} 