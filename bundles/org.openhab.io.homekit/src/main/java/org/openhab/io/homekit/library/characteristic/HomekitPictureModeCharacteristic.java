package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import java.util.Set;

/**
 * HomeKit Picture Mode Characteristic.
 * This characteristic represents the picture mode for a display device.
 *
 * See the official HomeKit documentation for details.
 */
@HomekitCharacteristicType(type = "000000E2-0000-1000-8000-0026BB765291", name = "Picture Mode", tag = "pictureMode")
@NonNullByDefault
public class HomekitPictureModeCharacteristic extends HomekitIntegerCharacteristic {
    public enum PictureMode {
        OTHER(0),
        STANDARD(1),
        CALIBRATED(2),
        CALIBRATED_DARK(3),
        VIVID(4),
        GAME(5),
        COMPUTER(6),
        CUSTOM(7);
        
        private final int code;
        
        PictureMode(int code) {
            this.code = code;
        }
        
        public int getCode() {
            return code;
        }
        
        public static PictureMode fromCode(int code) {
            for (PictureMode m : values()) {
                if (m.code == code) {
                    return m;
                }
            }
            return OTHER;
        }
    }

    public HomekitPictureModeCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 0, 7, "");
        withInstanceId(instanceId)
            .withPairedRead(true)
            .withPairedWrite(true)
            .withEvents(true)
            .withDescription("Picture Mode");
    }

    public HomekitPictureModeCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= 0 && value <= 7;
    }

    @Override
    public Set<Integer> getAllowedValues() {
        return Set.of(
            PictureMode.OTHER.getCode(),
            PictureMode.STANDARD.getCode(),
            PictureMode.CALIBRATED.getCode(),
            PictureMode.CALIBRATED_DARK.getCode(),
            PictureMode.VIVID.getCode(),
            PictureMode.GAME.getCode(),
            PictureMode.COMPUTER.getCode(),
            PictureMode.CUSTOM.getCode()
        );
    }
} 