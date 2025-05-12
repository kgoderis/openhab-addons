package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Security System Current State Characteristic.
 * This characteristic represents the current state of a security system.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/SecuritySystemCurrentState">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "00000066-0000-1000-8000-0026BB765291", name = "Security System Current State", tag = "securitySystemCurrentState")
@NonNullByDefault
public class HomekitSecuritySystemCurrentStateCharacteristic extends HomekitEnumCharacteristic {
    public enum SecuritySystemCurrentState {
        STAY_ARM(0),
        AWAY_ARM(1),
        NIGHT_ARM(2),
        DISARMED(3),
        ALARM_TRIGGERED(4);
        
        private final int code;
        
        SecuritySystemCurrentState(int code) {
            this.code = code;
        }
        
        public int getCode() {
            return code;
        }
        
        public static SecuritySystemCurrentState fromCode(int code) {
            for (SecuritySystemCurrentState s : values()) {
                if (s.code == code) return s;
            }
            return DISARMED;
        }
    }

    public HomekitSecuritySystemCurrentStateCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, SecuritySystemCurrentState.values().length);
        withInstanceId(instanceId)
            .withPairedWrite(false)
            .withPairedRead(true)
            .withEvents(true)
            .withDescription("Security System Current State");
    }

    public HomekitSecuritySystemCurrentStateCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= 0 && value < SecuritySystemCurrentState.values().length;
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(
            SecuritySystemCurrentState.STAY_ARM.getCode(),
            SecuritySystemCurrentState.AWAY_ARM.getCode(),
            SecuritySystemCurrentState.NIGHT_ARM.getCode(),
            SecuritySystemCurrentState.DISARMED.getCode(),
            SecuritySystemCurrentState.ALARM_TRIGGERED.getCode()
        );
    }
} 