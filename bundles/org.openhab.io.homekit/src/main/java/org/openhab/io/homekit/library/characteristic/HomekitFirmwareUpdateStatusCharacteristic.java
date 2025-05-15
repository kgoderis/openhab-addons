package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Firmware Update Status Characteristic.
 * This characteristic represents the status of a firmware update.
 * The status can be one of: IDLE, DOWNLOADING, INSTALLING, SUCCESS, or FAILED.
 *
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/homekit/hap-characteristic-types/firmware-update-status">HAP
 *      Specification</a>
 */
@HomekitCharacteristicType(type = "00000235-0000-1000-8000-0026BB765291", name = "Firmware Update Status", tag = "firmwareUpdateStatus", acceptedItemTypes = {
        "Number", "String" })
@NonNullByDefault
public class HomekitFirmwareUpdateStatusCharacteristic extends HomekitEnumCharacteristic {
    public enum FirmwareUpdateStatus {
        IDLE(0),
        DOWNLOADING(1),
        INSTALLING(2),
        SUCCESS(3),
        FAILED(4);

        private final int value;

        FirmwareUpdateStatus(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static FirmwareUpdateStatus fromValue(int value) {
            for (FirmwareUpdateStatus status : values()) {
                if (status.value == value) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Invalid firmware update status value: " + value);
        }
    }

    public HomekitFirmwareUpdateStatusCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, FirmwareUpdateStatus.FAILED.getValue());
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(false).withEvents(true)
                .withDescription("Firmware Update Status");
    }

    public HomekitFirmwareUpdateStatusCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= FirmwareUpdateStatus.IDLE.getValue()
                && value <= FirmwareUpdateStatus.FAILED.getValue();
    }
}
