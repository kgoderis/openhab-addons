package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitLongCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Selected Diagnostics Modes Characteristic.
 * This characteristic represents the selected diagnostics modes.
 *
 * See the official HomeKit documentation for details.
 */
@HomekitCharacteristicType(type = "0000024D-0000-1000-8000-0026BB765291", name = "Selected Diagnostics Modes", tag = "selectedDiagnosticsModes")
@NonNullByDefault
public class HomekitSelectedDiagnosticsModesCharacteristic extends HomekitLongCharacteristic {
    public HomekitSelectedDiagnosticsModesCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 0L, 0xFFFFFFFFL, 1L);
        withInstanceId(instanceId)
            .withPairedRead(true)
            .withPairedWrite(true)
            .withDescription("Selected Diagnostics Modes");
    }

    public HomekitSelectedDiagnosticsModesCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }
} 