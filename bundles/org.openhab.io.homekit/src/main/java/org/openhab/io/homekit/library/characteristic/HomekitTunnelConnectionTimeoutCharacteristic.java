package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Tunnel Connection Timeout Characteristic.
 * This characteristic represents the tunnel connection timeout for a device.
 * The timeout is expressed in seconds, ranging from 0 to 3600 seconds (1 hour).
 * This is used to specify how long a tunnel connection should remain active before timing out.
 *
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "00000061-0000-1000-8000-0026BB765291", name = "Tunnel Connection Timeout", tag = "tunnelConnectionTimeout", acceptedItemTypes = {
        "Number" })
@NonNullByDefault
public class HomekitTunnelConnectionTimeoutCharacteristic extends HomekitIntegerCharacteristic {
    /**
     * Creates a new Tunnel Connection Timeout characteristic.
     * The value range is 0 to 3600 seconds.
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param instanceId The instance ID for this characteristic
     */
    public HomekitTunnelConnectionTimeoutCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, 0, 3600, "seconds");
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(true).withEvents(true)
                .withDescription("Tunnel Connection Timeout");
    }

    /**
     * Creates a new Tunnel Connection Timeout characteristic from a JSON value.
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param value The JSON value to initialize the characteristic with
     */
    public HomekitTunnelConnectionTimeoutCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= 0 && value <= 3600;
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Collections.emptySet();
    }
}
