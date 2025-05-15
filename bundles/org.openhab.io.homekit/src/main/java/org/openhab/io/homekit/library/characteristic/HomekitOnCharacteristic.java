package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitBooleanCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit On Characteristic.
 * This characteristic represents the on/off state of a device.
 * When true, the device is on; when false, the device is off.
 *
 * @author Karel Goderis - Initial Contribution
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@NonNullByDefault
@HomekitCharacteristicType(type = "00000025-0000-1000-8000-0026BB765291", name = "On", tag = "on", acceptedItemTypes = {
        "Switch", "Contact" })
public class HomekitOnCharacteristic extends HomekitBooleanCharacteristic {

    public HomekitOnCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(true).withEvents(true).withDescription("On");
    }

    public HomekitOnCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public State toState(@Nullable Boolean value) {
        return OnOffType.from(value != null && value);
    }
}
