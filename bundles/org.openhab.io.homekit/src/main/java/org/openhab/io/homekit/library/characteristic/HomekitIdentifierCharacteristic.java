package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitLongCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Identifier Characteristic.
 * This characteristic represents a unique identifier for the accessory.
 *
 * @see <a href=\"https://developer.apple.com/documentation/HomeKit\">HAP Specification</a>
 * @author Karel Goderis - Initial Contribution
 */
@NonNullByDefault
@HomekitCharacteristicType(type = "000000E6-0000-1000-8000-0026BB765291", name = "Identifier", tag = "identifier")
public class HomekitIdentifierCharacteristic extends HomekitLongCharacteristic {
    public HomekitIdentifierCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 0L, Long.MAX_VALUE, 1L);
        withInstanceId(instanceId)
            .withPairedWrite(false)
            .withPairedRead(true)
            .withEvents(true)
            .withDescription("Identifier");
    }

    public HomekitIdentifierCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Long value) {
        return value != null && value >= 0L;
    }
} 