package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Current Fan State Characteristic.
 * This characteristic represents the current state of a fan.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/CurrentFanState">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "000000AF-0000-1000-8000-0026BB765291", name = "Current Fan State", tag = "currentFanState")
@NonNullByDefault
public class HomekitCurrentFanStateCharacteristic extends HomekitEnumCharacteristic {
    public enum FanState {
        INACTIVE(0),
        IDLE(1),
        BLOWING_AIR(2);
        
        private final int code;
        
        FanState(int code) {
            this.code = code;
        }
        
        public int getCode() {
            return code;
        }
        
        public static FanState fromCode(int code) {
            for (FanState s : values()) {
                if (s.code == code) {
                    return s;
                }
            }
            return INACTIVE;
        }
    }

    public HomekitCurrentFanStateCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, FanState.values().length);
        withInstanceId(instanceId)
            .withPairedWrite(false)
            .withPairedRead(true)
            .withEvents(true)
            .withDescription("Current Fan State");
    }

    public HomekitCurrentFanStateCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= 0 && value < FanState.values().length;
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(
            FanState.INACTIVE.getCode(),
            FanState.IDLE.getCode(),
            FanState.BLOWING_AIR.getCode()
        );
    }
} 