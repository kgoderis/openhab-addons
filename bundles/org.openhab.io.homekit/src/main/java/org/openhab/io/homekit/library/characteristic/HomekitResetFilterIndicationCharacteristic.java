package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Reset Filter Indication Characteristic.
 * @see <a href="https://developers.homebridge.io/#/characteristic/ResetFilterIndication">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "000000AD-0000-1000-8000-0026BB765291", name = "Reset Filter Indication", tag = "resetFilterIndication")
@NonNullByDefault
public class HomekitResetFilterIndicationCharacteristic extends HomekitIntegerCharacteristic {

    public HomekitResetFilterIndicationCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 1, 1, "");
        withInstanceId(instanceId)
            .withPairedWrite(true)
            .withPairedRead(false)
            .withEvents(false)
            .withDescription("Reset Filter Indication");
    }

    public HomekitResetFilterIndicationCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value == 1;
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(1);
    }
} 