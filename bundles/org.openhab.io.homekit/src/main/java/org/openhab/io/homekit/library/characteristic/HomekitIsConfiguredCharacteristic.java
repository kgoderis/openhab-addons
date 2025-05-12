package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitBooleanCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Is Configured Characteristic.
 * @see <a href="https://developers.homebridge.io/#/characteristic/IsConfigured">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "000000D6-0000-1000-8000-0026BB765291", name = "Is Configured", tag = "isConfigured")
@NonNullByDefault
public class HomekitIsConfiguredCharacteristic extends HomekitBooleanCharacteristic {

    public HomekitIsConfiguredCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId)
            .withPairedWrite(false)
            .withPairedRead(true)
            .withEvents(true)
            .withDescription("Is Configured");
    }

    public HomekitIsConfiguredCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }
} 