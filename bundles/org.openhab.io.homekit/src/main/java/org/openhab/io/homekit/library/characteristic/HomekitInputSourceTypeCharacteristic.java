package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Input Source Type Characteristic.
 * @see <a href="https://developers.homebridge.io/#/characteristic/InputSourceType">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "000000DB-0000-1000-8000-0026BB765291", name = "Input Source Type", tag = "inputSourceType")
@NonNullByDefault
public class HomekitInputSourceTypeCharacteristic extends HomekitIntegerCharacteristic {
    public enum InputSourceType {
        OTHER(0),
        HOME_SCREEN(1),
        TUNER(2),
        HDMI(3),
        COMPOSITE_VIDEO(4),
        S_VIDEO(5),
        COMPONENT_VIDEO(6),
        DVI(7),
        AIRPLAY(8),
        USB(9),
        APPLICATION(10);
        
        private final int code;
        
        InputSourceType(int code) {
            this.code = code;
        }
        
        public int getCode() {
            return code;
        }
        
        public static InputSourceType fromCode(int code) {
            for (InputSourceType s : values()) {
                if (s.code == code) {
                    return s;
                }
            }
            return OTHER;
        }
    }

    public HomekitInputSourceTypeCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 0, 10, "");
        withInstanceId(instanceId)
            .withPairedWrite(false)
            .withPairedRead(true)
            .withEvents(true)
            .withDescription("Input Source Type");
    }

    public HomekitInputSourceTypeCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= 0 && value <= 10;
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(
            InputSourceType.OTHER.getCode(),
            InputSourceType.HOME_SCREEN.getCode(),
            InputSourceType.TUNER.getCode(),
            InputSourceType.HDMI.getCode(),
            InputSourceType.COMPOSITE_VIDEO.getCode(),
            InputSourceType.S_VIDEO.getCode(),
            InputSourceType.COMPONENT_VIDEO.getCode(),
            InputSourceType.DVI.getCode(),
            InputSourceType.AIRPLAY.getCode(),
            InputSourceType.USB.getCode(),
            InputSourceType.APPLICATION.getCode()
        );
    }
} 