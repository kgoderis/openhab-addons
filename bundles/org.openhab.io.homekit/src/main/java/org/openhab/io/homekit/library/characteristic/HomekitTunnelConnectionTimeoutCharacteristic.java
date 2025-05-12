package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Tunnel Connection Timeout Characteristic.
 * @see <a href="https://developers.homebridge.io/#/characteristic/TunnelConnectionTimeout">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "00000061-0000-1000-8000-0026BB765291", name = "Tunnel Connection Timeout", tag = "tunnelConnectionTimeout")
@NonNullByDefault
public class HomekitTunnelConnectionTimeoutCharacteristic extends HomekitIntegerCharacteristic {

    public HomekitTunnelConnectionTimeoutCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 0, Integer.MAX_VALUE, "s");
        withInstanceId(instanceId)
            .withPairedWrite(true)
            .withPairedRead(true)
            .withEvents(true)
            .withDescription("Tunnel Connection Timeout");
    }

    public HomekitTunnelConnectionTimeoutCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
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