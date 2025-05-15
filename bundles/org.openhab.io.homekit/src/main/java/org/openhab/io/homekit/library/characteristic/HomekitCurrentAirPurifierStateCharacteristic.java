package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * Current Air Purifier State characteristic.
 * This characteristic represents the current state of an air purifier.
 *
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 * @author Karel Goderis - Initial contribution
 */
@HomekitCharacteristicType(type = "000000A9-0000-1000-8000-0026BB765291", name = "Current Air Purifier State", tag = "currentAirPurifierState", acceptedItemTypes = {"Number", "String"})
@NonNullByDefault
public class HomekitCurrentAirPurifierStateCharacteristic extends HomekitEnumCharacteristic {
    /**
     * Enum representing the possible states of an air purifier.
     * INACTIVE (0): The air purifier is not active
     * IDLE (1): The air purifier is idle
     * PURIFYING_AIR (2): The air purifier is actively purifying air
     */
    public enum AirPurifierState {
        INACTIVE(0),
        IDLE(1),
        PURIFYING_AIR(2);

        private final int code;

        AirPurifierState(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static AirPurifierState fromCode(int code) {
            for (AirPurifierState s : values()) {
                if (s.code == code) {
                    return s;
                }
            }
            return INACTIVE;
        }
    }

    /**
     * Creates a new Current Air Purifier State characteristic.
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param instanceId The instance ID for this characteristic
     */
    public HomekitCurrentAirPurifierStateCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, AirPurifierState.values().length);
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(true)
                .withDescription("Current Air Purifier State");
    }

    /**
     * Creates a new Current Air Purifier State characteristic from a JSON value.
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param value The JSON value to initialize the characteristic with
     */
    public HomekitCurrentAirPurifierStateCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    /**
     * Checks if the given value is a valid air purifier state.
     *
     * @param value the integer value to check
     * @return true if the value corresponds to a valid air purifier state, false otherwise
     */
    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= 0 && value < AirPurifierState.values().length;
    }

    /**
     * Gets the set of all valid air purifier state values.
     *
     * @return a set containing all valid air purifier state codes
     */
    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(AirPurifierState.INACTIVE.getCode(), AirPurifierState.IDLE.getCode(),
                AirPurifierState.PURIFYING_AIR.getCode());
    }
}
