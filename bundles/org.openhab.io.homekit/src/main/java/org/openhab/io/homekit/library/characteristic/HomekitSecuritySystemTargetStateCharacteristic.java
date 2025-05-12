package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Security System Target State Characteristic.
 * This characteristic represents the target state for a security system.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/SecuritySystemTargetState">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "00000067-0000-1000-8000-0026BB765291", name = "Security System Target State", tag = "securitySystemTargetState")
@NonNullByDefault
public class HomekitSecuritySystemTargetStateCharacteristic extends HomekitEnumCharacteristic {
    public enum SecuritySystemTargetState {
        STAY_ARM(0),
        AWAY_ARM(1),
        NIGHT_ARM(2),
        DISARM(3);
        
        private final int code;
        
        SecuritySystemTargetState(int code) {
            this.code = code;
        }
        
        public int getCode() {
            return code;
        }
        
        public static SecuritySystemTargetState fromCode(int code) {
            for (SecuritySystemTargetState s : values()) {
                if (s.code == code) return s;
            }
            return DISARM;
        }
    }

    public HomekitSecuritySystemTargetStateCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, SecuritySystemTargetState.values().length);
        withInstanceId(instanceId)
            .withPairedWrite(true)
            .withPairedRead(true)
            .withEvents(true)
            .withDescription("Security System Target State");
    }

    public HomekitSecuritySystemTargetStateCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= 0 && value < SecuritySystemTargetState.values().length;
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(
            SecuritySystemTargetState.STAY_ARM.getCode(),
            SecuritySystemTargetState.AWAY_ARM.getCode(),
            SecuritySystemTargetState.NIGHT_ARM.getCode(),
            SecuritySystemTargetState.DISARM.getCode()
        );
    }
} 