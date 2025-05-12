/**
 *
 */
package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitBooleanCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * Outlet In Use characteristic.
 * This characteristic represents whether the outlet is currently in use.
 *
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 * @author Karel Goderis - Initial contribution
 */
@NonNullByDefault
@HomekitCharacteristicType(type = "00000026-0000-1000-8000-0026BB765291", name = "Outlet In Use", tag = "outletInUse")
public class HomekitOutletInUseCharacteristic extends HomekitBooleanCharacteristic {

    public HomekitOutletInUseCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(true)
                .withDescription("Outlet In Use");
    }

    public HomekitOutletInUseCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }
}
