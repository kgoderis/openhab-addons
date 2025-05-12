package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Input Device Type Characteristic.
 * @see <a href="https://developers.homebridge.io/#/characteristic/InputDeviceType">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "000000DC-0000-1000-8000-0026BB765291", name = "Input Device Type", tag = "inputDeviceType")
@NonNullByDefault
public class HomekitInputDeviceTypeCharacteristic extends HomekitIntegerCharacteristic {
    public enum InputDeviceType {
        OTHER(0),
        TV(1),
        RECORDING(2),
        TUNER(3),
        PLAYBACK(4),
        AUDIO_SYSTEM(5);
        
        private final int code;
        
        InputDeviceType(int code) {
            this.code = code;
        }
        
        public int getCode() {
            return code;
        }
        
        public static InputDeviceType fromCode(int code) {
            for (InputDeviceType s : values()) {
                if (s.code == code) {
                    return s;
                }
            }
            return OTHER;
        }
    }

    public HomekitInputDeviceTypeCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 0, 5, "");
        withInstanceId(instanceId)
            .withPairedWrite(false)
            .withPairedRead(true)
            .withEvents(true)
            .withDescription("Input Device Type");
    }

    public HomekitInputDeviceTypeCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= 0 && value <= 5;
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(
            InputDeviceType.OTHER.getCode(),
            InputDeviceType.TV.getCode(),
            InputDeviceType.RECORDING.getCode(),
            InputDeviceType.TUNER.getCode(),
            InputDeviceType.PLAYBACK.getCode(),
            InputDeviceType.AUDIO_SYSTEM.getCode()
        );
    }
} 