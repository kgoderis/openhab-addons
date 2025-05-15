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
 * The state can be one of: STAY_ARM (0), AWAY_ARM (1), NIGHT_ARM (2), DISARMED (3), or ALARM_TRIGGERED (4).
 *
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "00000066-0000-1000-8000-0026BB765291", name = "Security System Current State", tag = "securitySystemCurrentState", acceptedItemTypes = {"Number", "String"})
@NonNullByDefault
public class HomekitSecuritySystemCurrentStateCharacteristic extends HomekitEnumCharacteristic {
    public enum SecuritySystemCurrentState {
        STAY_ARM(0),
        AWAY_ARM(1),
        NIGHT_ARM(2),
        DISARMED(3),
        ALARM_TRIGGERED(4);

        private final int value;

        SecuritySystemCurrentState(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static SecuritySystemCurrentState fromValue(int value) {
            for (SecuritySystemCurrentState state : values()) {
                if (state.value == value) {
                    return state;
                }
            }
            throw new IllegalArgumentException("Invalid Security System Current State value: " + value);
        }
    }

    public HomekitSecuritySystemCurrentStateCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, 5);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(false).withEvents(true)
                .withDescription("Security System Current State");
    }

    public HomekitSecuritySystemCurrentStateCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        if (value == null)
            return false;
        try {
            SecuritySystemCurrentState.fromValue(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        java.util.Set<Integer> allowed = new java.util.HashSet<>();
        for (SecuritySystemCurrentState state : SecuritySystemCurrentState.values()) {
            allowed.add(state.getValue());
        }
        return allowed;
    }
}
