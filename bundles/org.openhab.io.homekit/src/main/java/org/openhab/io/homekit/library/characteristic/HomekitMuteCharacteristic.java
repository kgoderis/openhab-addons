package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitBooleanCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Mute Characteristic.
 * This characteristic represents the mute state of an accessory.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/Mute">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "0000011A-0000-1000-8000-0026BB765291", name = "Mute", tag = "mute")
@NonNullByDefault
public class HomekitMuteCharacteristic extends HomekitBooleanCharacteristic {
    public HomekitMuteCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId).withPairedWrite(true).withPairedRead(true).withEvents(true)
            .withDescription("Mute");
    }
    public HomekitMuteCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }
} 