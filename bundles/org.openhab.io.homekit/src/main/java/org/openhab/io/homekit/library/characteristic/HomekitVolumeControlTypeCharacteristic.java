package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Volume Control Type Characteristic.
 * This characteristic represents the type of volume control available on a device.
 * The type can be one of: NONE, RELATIVE, RELATIVE_WITH_CURRENT, or ABSOLUTE.
 *
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(
    type = "000000E9-0000-1000-8000-0026BB765291",
    name = "Volume Control Type",
    tag = "volumeControlType",
    acceptedItemTypes = {"Number", "String"}
)
@NonNullByDefault
public class HomekitVolumeControlTypeCharacteristic extends HomekitEnumCharacteristic {
    public enum VolumeControlType {
        NONE(0),
        RELATIVE(1),
        RELATIVE_WITH_CURRENT(2),
        ABSOLUTE(3);

        private final int value;

        VolumeControlType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static VolumeControlType fromValue(int value) {
            for (VolumeControlType type : values()) {
                if (type.value == value) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Invalid Volume Control Type value: " + value);
        }
    }

    public HomekitVolumeControlTypeCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, VolumeControlType.values().length);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(false).withEvents(true)
                .withDescription("Volume Control Type");
    }

    public HomekitVolumeControlTypeCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        if (value == null) {
            return false;
        }
        try {
            VolumeControlType.fromValue(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(VolumeControlType.NONE.getValue(), VolumeControlType.RELATIVE.getValue(),
                VolumeControlType.RELATIVE_WITH_CURRENT.getValue(), VolumeControlType.ABSOLUTE.getValue());
    }
}
