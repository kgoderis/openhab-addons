package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Security System Alarm Level Characteristic.
 * This characteristic represents the alarm level for a security system.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/SecuritySystemAlarmLevel">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "0000008F-0000-1000-8000-0026BB765291", name = "Security System Alarm Level", tag = "securitySystemAlarmLevel")
@NonNullByDefault
public class HomekitSecuritySystemAlarmLevelCharacteristic extends HomekitEnumCharacteristic {
    public enum SecuritySystemAlarmLevel {
        NONE(0),
        WARNING(1),
        CRITICAL(2);
        
        private final int code;
        
        SecuritySystemAlarmLevel(int code) {
            this.code = code;
        }
        
        public int getCode() {
            return code;
        }
        
        public static SecuritySystemAlarmLevel fromCode(int code) {
            for (SecuritySystemAlarmLevel s : values()) {
                if (s.code == code) return s;
            }
            return NONE;
        }
    }

    public HomekitSecuritySystemAlarmLevelCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, SecuritySystemAlarmLevel.values().length);
        withInstanceId(instanceId)
            .withPairedWrite(false)
            .withPairedRead(true)
            .withEvents(true)
            .withDescription("Security System Alarm Level");
    }

    public HomekitSecuritySystemAlarmLevelCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= 0 && value < SecuritySystemAlarmLevel.values().length;
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(
            SecuritySystemAlarmLevel.NONE.getCode(),
            SecuritySystemAlarmLevel.WARNING.getCode(),
            SecuritySystemAlarmLevel.CRITICAL.getCode()
        );
    }
} 