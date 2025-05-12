package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitBooleanCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Administrator Only Access Characteristic.
 * This characteristic indicates whether the accessory can only be accessed by administrators.
 * When true, only administrators can access the accessory; when false, all users can access it.
 *
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "00000001-0000-1000-8000-0026BB765291", name = "Administrator Only Access", tag = "administratorOnlyAccess")
@NonNullByDefault
public class HomekitAdministratorOnlyAccessCharacteristic extends HomekitBooleanCharacteristic {

    public HomekitAdministratorOnlyAccessCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId)
            .withPairedRead(true)
            .withPairedWrite(true)
            .withEvents(true)
            .withDescription("Administrator Only Access");
    }

    public HomekitAdministratorOnlyAccessCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }
} 