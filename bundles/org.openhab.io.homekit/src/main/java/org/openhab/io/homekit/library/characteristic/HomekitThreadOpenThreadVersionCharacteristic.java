package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitStringCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Thread OpenThread Version Characteristic.
 * This characteristic represents the OpenThread version.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/ThreadOpenThreadVersion">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "00000706-0000-1000-8000-0026BB765291", name = "Thread OpenThread Version", tag = "threadOpenThreadVersion")
@NonNullByDefault
public class HomekitThreadOpenThreadVersionCharacteristic extends HomekitStringCharacteristic {
    public HomekitThreadOpenThreadVersionCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId)
            .withPairedRead(true)
            .withPairedWrite(false)
            .withEvents(true)
            .withDescription("Thread OpenThread Version");
    }

    public HomekitThreadOpenThreadVersionCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }
} 