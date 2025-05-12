package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import java.util.Set;

/**
 * HomeKit HomeKit Camera Active Characteristic.
 * This characteristic represents whether the HomeKit camera is active or not.
 *
 * See the official HomeKit documentation for details.
 */
@HomekitCharacteristicType(type = "0000021B-0000-1000-8000-0026BB765291", name = "HomeKit Camera Active", tag = "homeKitCameraActive")
@NonNullByDefault
public class HomekitHomeKitCameraActiveCharacteristic extends HomekitIntegerCharacteristic {
    public static final int OFF = 0;
    public static final int ON = 1;

    public HomekitHomeKitCameraActiveCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 0, 1, "");
        withInstanceId(instanceId)
            .withPairedRead(true)
            .withPairedWrite(true)
            .withEvents(true)
            .withDescription("HomeKit Camera Active");
    }

    public HomekitHomeKitCameraActiveCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && (value == OFF || value == ON);
    }

    @Override
    public Set<Integer> getAllowedValues() {
        return Set.of(OFF, ON);
    }
} 