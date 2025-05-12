package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import java.util.Set;

/**
 * HomeKit Volume Control Type Characteristic.
 * This characteristic represents the type of volume control available on a device.
 *
 * See the official HomeKit documentation for details.
 */
@HomekitCharacteristicType(type = "000000E9-0000-1000-8000-0026BB765291", name = "Volume Control Type", tag = "volumeControlType")
@NonNullByDefault
public class HomekitVolumeControlTypeCharacteristic extends HomekitIntegerCharacteristic {
    public enum VolumeControlType {
        NONE(0),
        RELATIVE(1),
        RELATIVE_WITH_CURRENT(2),
        ABSOLUTE(3);
        
        private final int code;
        
        VolumeControlType(int code) {
            this.code = code;
        }
        
        public int getCode() {
            return code;
        }
        
        public static VolumeControlType fromCode(int code) {
            for (VolumeControlType t : values()) {
                if (t.code == code) {
                    return t;
                }
            }
            return NONE;
        }
    }

    public HomekitVolumeControlTypeCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 0, 3, "");
        withInstanceId(instanceId)
            .withPairedRead(true)
            .withPairedWrite(false)
            .withEvents(true)
            .withDescription("Volume Control Type");
    }

    public HomekitVolumeControlTypeCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && VolumeControlType.fromCode(value) != null;
    }

    @Override
    public Set<Integer> getAllowedValues() {
        return Set.of(
            VolumeControlType.NONE.getCode(),
            VolumeControlType.RELATIVE.getCode(),
            VolumeControlType.RELATIVE_WITH_CURRENT.getCode(),
            VolumeControlType.ABSOLUTE.getCode()
        );
    }
} 