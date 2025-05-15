package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitBooleanCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Programmable Switch Output State Characteristic.
 * This characteristic represents the output state for a programmable switch.
 *
 * @see <a href=\"https://developer.apple.com/documentation/HomeKit\">HAP Specification</a>
 * @author Karel Goderis - Initial Contribution
 */
@HomekitCharacteristicType(type = "00000074-0000-1000-8000-0026BB765291", name = "Programmable Switch Output State", tag = "programmableSwitchOutputState")
@NonNullByDefault
public class HomekitProgrammableSwitchOutputStateCharacteristic extends HomekitBooleanCharacteristic {
    public HomekitProgrammableSwitchOutputStateCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(true)
                .withDescription("Programmable Switch Output State");
    }

    public HomekitProgrammableSwitchOutputStateCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }
}
