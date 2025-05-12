package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitStringCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Configured Name Characteristic.
 * @see <a href="https://developers.homebridge.io/#/characteristic/ConfiguredName">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "000000E3-0000-1000-8000-0026BB765291", name = "Configured Name", tag = "configuredName")
@NonNullByDefault
public class HomekitConfiguredNameCharacteristic extends HomekitStringCharacteristic {

    public HomekitConfiguredNameCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId)
            .withPairedWrite(true)
            .withPairedRead(true)
            .withEvents(true)
            .withDescription("Configured Name");
    }

    public HomekitConfiguredNameCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }
} 