package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitBooleanCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Tunneled Accessory Advertising Characteristic.
 * @see <a href="https://developers.homebridge.io/#/characteristic/TunneledAccessoryAdvertising">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "00000060-0000-1000-8000-0026BB765291", name = "Tunneled Accessory Advertising", tag = "tunneledAccessoryAdvertising")
@NonNullByDefault
public class HomekitTunneledAccessoryAdvertisingCharacteristic extends HomekitBooleanCharacteristic {

    public HomekitTunneledAccessoryAdvertisingCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId)
            .withPairedWrite(true)
            .withPairedRead(true)
            .withEvents(true)
            .withDescription("Tunneled Accessory Advertising");
    }

    public HomekitTunneledAccessoryAdvertisingCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }
} 