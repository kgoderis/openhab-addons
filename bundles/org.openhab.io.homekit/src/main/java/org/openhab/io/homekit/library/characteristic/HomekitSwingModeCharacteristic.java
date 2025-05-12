package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Swing Mode Characteristic.
 * This characteristic represents the swing mode for a device (e.g., fan).
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/SwingMode">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "000000B6-0000-1000-8000-0026BB765291", name = "Swing Mode", tag = "swingMode")
@NonNullByDefault
public class HomekitSwingModeCharacteristic extends HomekitEnumCharacteristic {
    public enum SwingMode {
        SWING_DISABLED(0),
        SWING_ENABLED(1);
        private final int code;
        SwingMode(int code) { this.code = code; }
        public int getCode() { return code; }
        public static SwingMode fromCode(int code) {
            for (SwingMode s : values()) {
                if (s.code == code) return s;
            }
            return SWING_DISABLED;
        }
    }
    public HomekitSwingModeCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, SwingMode.values().length);
        withInstanceId(instanceId)
            .withPairedWrite(true)
            .withPairedRead(true)
            .withEvents(true)
            .withDescription("Swing Mode");
    }
    public HomekitSwingModeCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }
    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= 0 && value < SwingMode.values().length;
    }
    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(
            SwingMode.SWING_DISABLED.getCode(),
            SwingMode.SWING_ENABLED.getCode()
        );
    }
} 