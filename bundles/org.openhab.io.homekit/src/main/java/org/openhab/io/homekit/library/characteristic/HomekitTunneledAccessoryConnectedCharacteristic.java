package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitBooleanCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Tunneled Accessory Connected Characteristic.
 * @see <a href="https://developers.homebridge.io/#/characteristic/TunneledAccessoryConnected">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "00000059-0000-1000-8000-0026BB765291", name = "Tunneled Accessory Connected", tag = "tunneledAccessoryConnected")
@NonNullByDefault
public class HomekitTunneledAccessoryConnectedCharacteristic extends HomekitBooleanCharacteristic {

    public HomekitTunneledAccessoryConnectedCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId)
            .withPairedWrite(true)
            .withPairedRead(true)
            .withEvents(true)
            .withDescription("Tunneled Accessory Connected");
    }

    public HomekitTunneledAccessoryConnectedCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }
} 