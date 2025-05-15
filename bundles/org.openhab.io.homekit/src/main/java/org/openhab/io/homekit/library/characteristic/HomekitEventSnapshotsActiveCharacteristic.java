package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitBooleanCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Event Snapshots Active Characteristic.
 * This characteristic represents whether event snapshots are active.
 *
 * @author Karel Goderis - Initial Contribution
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "00000223-0000-1000-8000-0026BB765291", name = "Event Snapshots Active", tag = "eventSnapshotsActive", acceptedItemTypes = {"Switch", "Contact"})
@NonNullByDefault
public class HomekitEventSnapshotsActiveCharacteristic extends HomekitBooleanCharacteristic {
    /**
     * Constructs a new Event Snapshots Active characteristic.
     *
     * @param service the HomeKit service this characteristic belongs to
     * @param eventManager the event manager for handling HomeKit events
     * @param instanceId the instance ID for this characteristic
     */
    public HomekitEventSnapshotsActiveCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(true).withEvents(true)
                .withDescription("Event Snapshots Active");
    }

    /**
     * Constructs a new Event Snapshots Active characteristic from a JSON value.
     *
     * @param service the HomeKit service this characteristic belongs to
     * @param eventManager the event manager for handling HomeKit events
     * @param value the JSON value to initialize the characteristic with
     */
    public HomekitEventSnapshotsActiveCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }
}
