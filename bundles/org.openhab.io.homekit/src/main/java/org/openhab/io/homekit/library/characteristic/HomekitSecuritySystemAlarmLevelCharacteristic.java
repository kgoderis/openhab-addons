package org.openhab.io.homekit.library.characteristic;

import java.util.Set;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Security System Alarm Level Characteristic.
 * <p>
 * This characteristic represents the alarm level for a security system, indicating the severity of an alarm event
 * (none, warning, or critical).
 * <p>
 * See the HomeKit Accessory Protocol (HAP) specification for details: https://developer.apple.com/documentation/HomeKit
 *
 * @author Karel Goderis
 */
@HomekitCharacteristicType(type = "0000008F-0000-1000-8000-0026BB765291", name = "Security System Alarm Level", tag = "securitySystemAlarmLevel", acceptedItemTypes = {
        "Number", "String" })
@NonNullByDefault
public class HomekitSecuritySystemAlarmLevelCharacteristic extends HomekitEnumCharacteristic {
    /**
     * Enum representing the possible alarm levels for a security system.
     */
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
                if (s.code == code)
                    return s;
            }
            return NONE;
        }
    }

    /**
     * Constructs a new Security System Alarm Level characteristic.
     *
     * @param service the HomeKit service this characteristic belongs to
     * @param eventManager the event manager for handling HomeKit events
     * @param instanceId the instance ID for this characteristic
     */
    public HomekitSecuritySystemAlarmLevelCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, SecuritySystemAlarmLevel.values().length);
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(true)
                .withDescription("Security System Alarm Level");
    }

    /**
     * Constructs a new Security System Alarm Level characteristic from a JSON value.
     *
     * @param service the HomeKit service this characteristic belongs to
     * @param eventManager the event manager for handling HomeKit events
     * @param value the JSON value to initialize the characteristic with
     */
    public HomekitSecuritySystemAlarmLevelCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    /**
     * Checks if the given value is an allowed alarm level.
     *
     * @param value the value to check
     * @return true if the value is allowed, false otherwise
     */
    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= 0 && value < SecuritySystemAlarmLevel.values().length;
    }

    /**
     * Returns the set of allowed alarm level values.
     *
     * @return the set of allowed values
     */
    @Override
    public Set<Integer> getAllowedValues() {
        return Set.of(SecuritySystemAlarmLevel.NONE.getCode(), SecuritySystemAlarmLevel.WARNING.getCode(),
                SecuritySystemAlarmLevel.CRITICAL.getCode());
    }
}
