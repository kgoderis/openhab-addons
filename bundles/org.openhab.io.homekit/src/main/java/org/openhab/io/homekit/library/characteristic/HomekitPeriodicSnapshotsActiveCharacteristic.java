package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitBooleanCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Periodic Snapshots Active Characteristic.
 * This characteristic represents whether periodic snapshots are active.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/PeriodicSnapshotsActive">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "00000225-0000-1000-8000-0026BB765291", name = "Periodic Snapshots Active", tag = "periodicSnapshotsActive")
@NonNullByDefault
public class HomekitPeriodicSnapshotsActiveCharacteristic extends HomekitBooleanCharacteristic {
    public HomekitPeriodicSnapshotsActiveCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId)
            .withPairedRead(true)
            .withPairedWrite(true)
            .withEvents(true)
            .withDescription("Periodic Snapshots Active");
    }

    public HomekitPeriodicSnapshotsActiveCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }
} 