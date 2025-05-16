package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitStringCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Staged Firmware Version Characteristic.
 * This characteristic represents the staged firmware version.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/StagedFirmwareVersion">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "00000249-0000-1000-8000-0026BB765291", name = "Staged Firmware Version", tag = "stagedFirmwareVersion", acceptedItemTypes = {
        "String" })
@NonNullByDefault
public class HomekitStagedFirmwareVersionCharacteristic extends HomekitStringCharacteristic {
    public HomekitStagedFirmwareVersionCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(false).withEvents(true)
                .withDescription("Staged Firmware Version");
    }

    public HomekitStagedFirmwareVersionCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }
}
