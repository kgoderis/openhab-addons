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
 * The state can be one of: STAY_ARM (0), AWAY_ARM (1), NIGHT_ARM (2), or DISARM (3).
 *
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "00000067-0000-1000-8000-0026BB765291", name = "Security System Target State", tag = "securitySystemTargetState", acceptedItemTypes = {"Number", "String"})
@NonNullByDefault
public class HomekitSecuritySystemTargetStateCharacteristic extends HomekitEnumCharacteristic {
    public enum SecuritySystemTargetState {
        STAY_ARM(0),
        AWAY_ARM(1),
        NIGHT_ARM(2),
        DISARM(3);

        private final int value;

        SecuritySystemTargetState(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static SecuritySystemTargetState fromValue(int value) {
            for (SecuritySystemTargetState state : values()) {
                if (state.value == value) {
                    return state;
                }
            }
            throw new IllegalArgumentException("Invalid Security System Target State value: " + value);
        }
    }

    public HomekitSecuritySystemTargetStateCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, 4);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(true).withEvents(true)
                .withDescription("Security System Target State");
    }

    public HomekitSecuritySystemTargetStateCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        if (value == null)
            return false;
        try {
            SecuritySystemTargetState.fromValue(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        java.util.Set<Integer> allowed = new java.util.HashSet<>();
        for (SecuritySystemTargetState state : SecuritySystemTargetState.values()) {
            allowed.add(state.getValue());
        }
        return allowed;
    }
}
