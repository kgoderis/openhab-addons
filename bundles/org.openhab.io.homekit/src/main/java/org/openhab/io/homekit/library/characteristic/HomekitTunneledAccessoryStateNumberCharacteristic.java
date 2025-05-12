package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Tunneled Accessory State Number Characteristic.
 * @see <a href="https://developers.homebridge.io/#/characteristic/TunneledAccessoryStateNumber">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "00000058-0000-1000-8000-0026BB765291", name = "Tunneled Accessory State Number", tag = "tunneledAccessoryStateNumber")
@NonNullByDefault
public class HomekitTunneledAccessoryStateNumberCharacteristic extends HomekitIntegerCharacteristic {

    public HomekitTunneledAccessoryStateNumberCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 0, Integer.MAX_VALUE, "");
        withInstanceId(instanceId)
            .withPairedWrite(false)
            .withPairedRead(true)
            .withEvents(true)
            .withDescription("Tunneled Accessory State Number");
    }

    public HomekitTunneledAccessoryStateNumberCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= 0;
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Collections.emptySet();
    }
} 