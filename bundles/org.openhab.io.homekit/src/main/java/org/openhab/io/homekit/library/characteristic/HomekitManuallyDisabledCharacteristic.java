package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitBooleanCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Manually Disabled Characteristic.
 * This characteristic represents whether the device has been manually disabled.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/ManuallyDisabled">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "00000227-0000-1000-8000-0026BB765291", name = "Manually Disabled", tag = "manuallyDisabled")
@NonNullByDefault
public class HomekitManuallyDisabledCharacteristic extends HomekitBooleanCharacteristic {
    public HomekitManuallyDisabledCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId)
            .withPairedRead(true)
            .withPairedWrite(true)
            .withEvents(true)
            .withDescription("Manually Disabled");
    }

    public HomekitManuallyDisabledCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }
} 