package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Current Slat State Characteristic.
 * This characteristic represents the current state of a slat (e.g., in a window blind).
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/CurrentSlatState">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "000000AA-0000-1000-8000-0026BB765291", name = "Current Slat State", tag = "currentSlatState")
@NonNullByDefault
public class HomekitCurrentSlatStateCharacteristic extends HomekitEnumCharacteristic {
    public enum SlatState {
        INACTIVE(0),
        IDLE(1),
        ROTATING(2);
        
        private final int code;
        
        SlatState(int code) {
            this.code = code;
        }
        
        public int getCode() {
            return code;
        }
        
        public static SlatState fromCode(int code) {
            for (SlatState s : values()) {
                if (s.code == code) {
                    return s;
                }
            }
            return INACTIVE;
        }
    }

    public HomekitCurrentSlatStateCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, SlatState.values().length);
        withInstanceId(instanceId)
            .withPairedWrite(false)
            .withPairedRead(true)
            .withEvents(true)
            .withDescription("Current Slat State");
    }

    public HomekitCurrentSlatStateCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= 0 && value < SlatState.values().length;
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(
            SlatState.INACTIVE.getCode(),
            SlatState.IDLE.getCode(),
            SlatState.ROTATING.getCode()
        );
    }
} 