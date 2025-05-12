package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Security System Alarm Type Characteristic.
 * This characteristic represents the type of alarm for a security system.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/SecuritySystemAlarmType">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "0000008E-0000-1000-8000-0026BB765291", name = "Security System Alarm Type", tag = "securitySystemAlarmType")
@NonNullByDefault
public class HomekitSecuritySystemAlarmTypeCharacteristic extends HomekitEnumCharacteristic {
    public enum SecuritySystemAlarmType {
        NONE(0),
        GENERAL(1),
        BURGLAR(2),
        FIRE(3),
        WATER(4),
        CARBON_MONOXIDE(5),
        POWER(6);
        
        private final int code;
        
        SecuritySystemAlarmType(int code) {
            this.code = code;
        }
        
        public int getCode() {
            return code;
        }
        
        public static SecuritySystemAlarmType fromCode(int code) {
            for (SecuritySystemAlarmType s : values()) {
                if (s.code == code) return s;
            }
            return NONE;
        }
    }

    public HomekitSecuritySystemAlarmTypeCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, SecuritySystemAlarmType.values().length);
        withInstanceId(instanceId)
            .withPairedWrite(false)
            .withPairedRead(true)
            .withEvents(true)
            .withDescription("Security System Alarm Type");
    }

    public HomekitSecuritySystemAlarmTypeCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= 0 && value < SecuritySystemAlarmType.values().length;
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(
            SecuritySystemAlarmType.NONE.getCode(),
            SecuritySystemAlarmType.GENERAL.getCode(),
            SecuritySystemAlarmType.BURGLAR.getCode(),
            SecuritySystemAlarmType.FIRE.getCode(),
            SecuritySystemAlarmType.WATER.getCode(),
            SecuritySystemAlarmType.CARBON_MONOXIDE.getCode(),
            SecuritySystemAlarmType.POWER.getCode()
        );
    }
} 