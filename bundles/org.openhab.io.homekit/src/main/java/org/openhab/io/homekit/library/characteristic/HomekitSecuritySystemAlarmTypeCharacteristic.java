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
 * The type can be one of: NO_ALARM (0), GENERAL_ALARM (1), or SMOKE_ALARM (2).
 *
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "0000008E-0000-1000-8000-0026BB765291", name = "Security System Alarm Type", tag = "securitySystemAlarmType", acceptedItemTypes = {
        "Number", "String" })
@NonNullByDefault
public class HomekitSecuritySystemAlarmTypeCharacteristic extends HomekitEnumCharacteristic {
    public enum SecuritySystemAlarmType {
        NO_ALARM(0),
        GENERAL_ALARM(1),
        SMOKE_ALARM(2);

        private final int value;

        SecuritySystemAlarmType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static SecuritySystemAlarmType fromValue(int value) {
            for (SecuritySystemAlarmType type : values()) {
                if (type.value == value) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Invalid Security System Alarm Type value: " + value);
        }
    }

    public HomekitSecuritySystemAlarmTypeCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, 3);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(false).withEvents(true)
                .withDescription("Security System Alarm Type");
    }

    public HomekitSecuritySystemAlarmTypeCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        if (value == null)
            return false;
        try {
            SecuritySystemAlarmType.fromValue(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        java.util.Set<Integer> allowed = new java.util.HashSet<>();
        for (SecuritySystemAlarmType type : SecuritySystemAlarmType.values()) {
            allowed.add(type.getValue());
        }
        return allowed;
    }
}
