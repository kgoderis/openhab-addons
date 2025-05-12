package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import java.util.Set;

/**
 * HomeKit Power Mode Selection Characteristic.
 * This characteristic represents the power mode selection for a device.
 *
 * See the official HomeKit documentation for details.
 */
@HomekitCharacteristicType(type = "000000DF-0000-1000-8000-0026BB765291", name = "Power Mode Selection", tag = "powerModeSelection")
@NonNullByDefault
public class HomekitPowerModeSelectionCharacteristic extends HomekitIntegerCharacteristic {
    public enum PowerMode {
        SHOW(0),
        HIDE(1);
        
        private final int code;
        
        PowerMode(int code) {
            this.code = code;
        }
        
        public int getCode() {
            return code;
        }
        
        public static PowerMode fromCode(int code) {
            for (PowerMode m : values()) {
                if (m.code == code) {
                    return m;
                }
            }
            return SHOW;
        }
    }

    public HomekitPowerModeSelectionCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 0, 1, "");
        withInstanceId(instanceId)
            .withPairedRead(true)
            .withPairedWrite(true)
            .withEvents(true)
            .withDescription("Power Mode Selection");
    }

    public HomekitPowerModeSelectionCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && (value == PowerMode.SHOW.getCode() || value == PowerMode.HIDE.getCode());
    }

    @Override
    public Set<Integer> getAllowedValues() {
        return Set.of(
            PowerMode.SHOW.getCode(),
            PowerMode.HIDE.getCode()
        );
    }
} 