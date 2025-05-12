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
 * @see <a href="https://developers.homebridge.io/#/characteristic/EventSnapshotsActive">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "00000223-0000-1000-8000-0026BB765291", name = "Event Snapshots Active", tag = "eventSnapshotsActive")
@NonNullByDefault
public class HomekitEventSnapshotsActiveCharacteristic extends HomekitBooleanCharacteristic {
    public HomekitEventSnapshotsActiveCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId)
            .withPairedRead(true)
            .withPairedWrite(true)
            .withEvents(true)
            .withDescription("Event Snapshots Active");
    }

    public HomekitEventSnapshotsActiveCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }
} 