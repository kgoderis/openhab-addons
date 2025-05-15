package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitStringCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Siri Engine Version Characteristic.
 * This characteristic represents the Siri engine version.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/SiriEngineVersion">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "0000025A-0000-1000-8000-0026BB765291", name = "Siri Engine Version", tag = "siriEngineVersion", acceptedItemTypes = {"String"})
@NonNullByDefault
public class HomekitSiriEngineVersionCharacteristic extends HomekitStringCharacteristic {
    public HomekitSiriEngineVersionCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(false).withEvents(true)
                .withDescription("Siri Engine Version");
    }

    public HomekitSiriEngineVersionCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }
}
