package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Firmware Update Readiness Characteristic.
 * This characteristic represents the readiness state for firmware updates.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/FirmwareUpdateReadiness">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "00000234-0000-1000-8000-0026BB765291", name = "Firmware Update Readiness", tag = "firmwareUpdateReadiness", acceptedItemTypes = {
        "Number", "String" })
@NonNullByDefault
public class HomekitFirmwareUpdateReadinessCharacteristic extends HomekitEnumCharacteristic {
    public static final int READY = 0;
    public static final int NOT_READY = 1;
    public static final int IN_PROGRESS = 2;

    public HomekitFirmwareUpdateReadinessCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, 2);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(false).withEvents(true)
                .withDescription("Firmware Update Readiness");
    }

    public HomekitFirmwareUpdateReadinessCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= READY && value <= IN_PROGRESS;
    }
}
