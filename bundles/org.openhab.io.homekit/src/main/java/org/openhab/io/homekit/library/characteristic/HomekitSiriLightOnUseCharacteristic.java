package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitBooleanCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Siri Light On Use Characteristic.
 * This characteristic represents whether the light turns on when Siri is used.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/SiriLightOnUse">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "00000258-0000-1000-8000-0026BB765291", name = "Siri Light On Use", tag = "siriLightOnUse", acceptedItemTypes = {
        "Switch", "Contact" })
@NonNullByDefault
public class HomekitSiriLightOnUseCharacteristic extends HomekitBooleanCharacteristic {
    public HomekitSiriLightOnUseCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(true).withEvents(true)
                .withDescription("Siri Light On Use");
    }

    public HomekitSiriLightOnUseCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }
}
