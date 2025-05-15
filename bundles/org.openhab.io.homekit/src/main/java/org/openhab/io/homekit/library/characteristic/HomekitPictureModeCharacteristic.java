package org.openhab.io.homekit.library.characteristic;

import java.util.Set;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Picture Mode Characteristic.
 * <p>
 * This characteristic represents the picture mode for a display device, allowing the user to select between different
 * picture modes as defined by the HAP specification.
 * <p>
 * See the HomeKit Accessory Protocol (HAP) specification for details: https://developer.apple.com/documentation/HomeKit
 *
 * @author Karel Goderis
 */
@HomekitCharacteristicType(type = "000000E2-0000-1000-8000-0026BB765291", name = "Picture Mode", tag = "pictureMode")
@NonNullByDefault
public class HomekitPictureModeCharacteristic extends HomekitIntegerCharacteristic {
    /**
     * Enum representing the possible picture modes.
     */
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

        /**
         * Returns the integer code for this picture mode.
         * 
         * @return the code
         */
        public int getCode() {
            return code;
        }

        /**
         * Returns the PictureMode enum for a given code.
         * 
         * @param code the code
         * @return the PictureMode
         */
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
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(true).withEvents(true)
                .withDescription("Picture Mode");
    }

    public HomekitPictureModeCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    /**
     * Checks if the given value is an allowed picture mode.
     * 
     * @param value the value to check
     * @return true if allowed, false otherwise
     */
    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= 0 && value <= 7;
    }

    /**
     * Returns the set of allowed picture mode values.
     * 
     * @return the set of allowed values
     */
    @Override
    public Set<Integer> getAllowedValues() {
        return Set.of(PictureMode.OTHER.getCode(), PictureMode.STANDARD.getCode(), PictureMode.CALIBRATED.getCode(),
                PictureMode.CALIBRATED_DARK.getCode(), PictureMode.VIVID.getCode(), PictureMode.GAME.getCode(),
                PictureMode.COMPUTER.getCode(), PictureMode.CUSTOM.getCode());
    }
}
