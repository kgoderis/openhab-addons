package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitBooleanCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Closed Captions Characteristic.
 * @see <a href="https://developers.homebridge.io/#/characteristic/ClosedCaptions">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "000000DD-0000-1000-8000-0026BB765291", name = "Closed Captions", tag = "closedCaptions")
@NonNullByDefault
public class HomekitClosedCaptionsCharacteristic extends HomekitBooleanCharacteristic {

    public HomekitClosedCaptionsCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId)
            .withPairedWrite(true)
            .withPairedRead(true)
            .withEvents(true)
            .withDescription("Closed Captions");
    }

    public HomekitClosedCaptionsCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }
} 