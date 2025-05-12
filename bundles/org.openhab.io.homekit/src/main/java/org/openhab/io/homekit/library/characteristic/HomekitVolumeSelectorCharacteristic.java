package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Volume Selector Characteristic.
 * This characteristic represents the volume selector for a device (increment/decrement).
 *
 * See the official HomeKit documentation for details.
 */
@HomekitCharacteristicType(type = "000000EA-0000-1000-8000-0026BB765291", name = "Volume Selector", tag = "volumeSelector")
@NonNullByDefault
public class HomekitVolumeSelectorCharacteristic extends HomekitEnumCharacteristic {
    public enum VolumeSelector {
        INCREMENT(0),
        DECREMENT(1);
        private final int code;
        VolumeSelector(int code) { this.code = code; }
        public int getCode() { return code; }
        public static VolumeSelector fromCode(int code) {
            for (VolumeSelector s : values()) {
                if (s.code == code) return s;
            }
            return INCREMENT;
        }
    }
    public HomekitVolumeSelectorCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, VolumeSelector.values().length);
        withInstanceId(instanceId)
            .withPairedWrite(true)
            .withPairedRead(true)
            .withEvents(true)
            .withDescription("Volume Selector");
    }
    public HomekitVolumeSelectorCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }
    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= 0 && value < VolumeSelector.values().length;
    }
    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(
            VolumeSelector.INCREMENT.getCode(),
            VolumeSelector.DECREMENT.getCode()
        );
    }
} 