package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import java.util.Set;

/**
 * HomeKit Managed Network Enable Characteristic.
 * This characteristic represents whether the managed network is enabled or disabled.
 *
 * See the official HomeKit documentation for details.
 */
@HomekitCharacteristicType(type = "00000215-0000-1000-8000-0026BB765291", name = "Managed Network Enable", tag = "managedNetworkEnable")
@NonNullByDefault
public class HomekitManagedNetworkEnableCharacteristic extends HomekitIntegerCharacteristic {
    public static final int DISABLED = 0;
    public static final int ENABLED = 1;

    public HomekitManagedNetworkEnableCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 0, 1, "");
        withInstanceId(instanceId)
            .withPairedRead(true)
            .withPairedWrite(true)
            .withEvents(true)
            .withTimedWrite(true)
            .withDescription("Managed Network Enable");
    }

    public HomekitManagedNetworkEnableCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && (value == DISABLED || value == ENABLED);
    }

    @Override
    public Set<Integer> getAllowedValues() {
        return Set.of(DISABLED, ENABLED);
    }
} 