package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Event Retransmission Maximum Characteristic.
 * This characteristic represents the maximum number of event retransmissions.
 *
 * @author Karel Goderis - Initial Contribution
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "0000023D-0000-1000-8000-0026BB765291", name = "Event Retransmission Maximum", tag = "eventRetransmissionMaximum", acceptedItemTypes = {
        "Number" })
@NonNullByDefault
public class HomekitEventRetransmissionMaximumCharacteristic extends HomekitIntegerCharacteristic {
    /**
     * Constructs a new Event Retransmission Maximum characteristic.
     *
     * @param service the HomeKit service this characteristic belongs to
     * @param eventManager the event manager for handling HomeKit events
     * @param instanceId the instance ID for this characteristic
     */
    public HomekitEventRetransmissionMaximumCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, 0, Integer.MAX_VALUE, "");
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(true).withEvents(true)
                .withDescription("Event Retransmission Maximum");
    }

    /**
     * Constructs a new Event Retransmission Maximum characteristic from a JSON value.
     *
     * @param service the HomeKit service this characteristic belongs to
     * @param eventManager the event manager for handling HomeKit events
     * @param value the JSON value to initialize the characteristic with
     */
    public HomekitEventRetransmissionMaximumCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    /**
     * Checks if the provided value is allowed for this characteristic.
     *
     * @param value the value to check
     * @return true if the value is not null and greater than or equal to 0, false otherwise
     */
    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= 0;
    }
}
