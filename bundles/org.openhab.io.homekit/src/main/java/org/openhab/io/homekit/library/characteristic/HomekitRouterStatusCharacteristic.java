package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import java.util.Set;

/**
 * HomeKit Router Status Characteristic.
 * This characteristic represents the router status (READY or NOT_READY).
 *
 * See the official HomeKit documentation for details.
 */
@HomekitCharacteristicType(type = "0000020E-0000-1000-8000-0026BB765291", name = "Router Status", tag = "routerStatus")
@NonNullByDefault
public class HomekitRouterStatusCharacteristic extends HomekitIntegerCharacteristic {
    public static final int READY = 0;
    public static final int NOT_READY = 1;

    public HomekitRouterStatusCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 0, 1, "");
        withInstanceId(instanceId)
            .withPairedRead(true)
            .withPairedWrite(false)
            .withEvents(true)
            .withDescription("Router Status");
    }

    public HomekitRouterStatusCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && (value == READY || value == NOT_READY);
    }

    @Override
    public Set<Integer> getAllowedValues() {
        return Set.of(READY, NOT_READY);
    }
} 