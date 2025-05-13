package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * Current Media State characteristic.
 * This characteristic represents the current state for a media device.
 *
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 * @author Karel Goderis - Initial contribution
 */
@HomekitCharacteristicType(type = "000000E0-0000-1000-8000-0026BB765291", name = "Current Media State", tag = "currentMediaState")
@NonNullByDefault
public class HomekitCurrentMediaStateCharacteristic extends HomekitIntegerCharacteristic {
    /**
     * Enum representing the possible states of a media device.
     * Each state has a corresponding integer code used in the HomeKit protocol.
     */
    public enum CurrentMediaState {
        /** Media is currently playing */
        PLAY(0),
        /** Media playback is paused */
        PAUSE(1),
        /** Media playback is stopped */
        STOP(2),
        /** Media is currently loading */
        LOADING(4),
        /** Media playback is interrupted */
        INTERRUPTED(5);
        
        private final int code;
        
        CurrentMediaState(int code) {
            this.code = code;
        }
        
        /**
         * Gets the integer code for this media state.
         *
         * @return the integer code
         */
        public int getCode() {
            return code;
        }
        
        /**
         * Converts an integer code to the corresponding CurrentMediaState.
         * If no matching state is found, returns STOP as default.
         *
         * @param code the integer code to convert
         * @return the corresponding CurrentMediaState, or STOP if not found
         */
        public static CurrentMediaState fromCode(int code) {
            for (CurrentMediaState s : values()) {
                if (s.code == code) {
                    return s;
                }
            }
            return STOP;
        }
    }

    /**
     * Creates a new Current Media State characteristic.
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param instanceId The instance ID for this characteristic
     */
    public HomekitCurrentMediaStateCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 0, 5, "");
        withInstanceId(instanceId)
            .withPairedWrite(false)
            .withPairedRead(true)
            .withEvents(true)
            .withDescription("Current Media State");
    }

    /**
     * Creates a new Current Media State characteristic from a JSON value.
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param value The JSON value to initialize the characteristic with
     */
    public HomekitCurrentMediaStateCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    /**
     * Checks if the given value is a valid media state.
     *
     * @param value the integer value to check
     * @return true if the value corresponds to a valid media state, false otherwise
     */
    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && (value == CurrentMediaState.PLAY.getCode() 
            || value == CurrentMediaState.PAUSE.getCode() 
            || value == CurrentMediaState.STOP.getCode() 
            || value == CurrentMediaState.LOADING.getCode() 
            || value == CurrentMediaState.INTERRUPTED.getCode());
    }

    /**
     * Gets the set of all valid media state values.
     *
     * @return a set containing all valid media state codes
     */
    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(
            CurrentMediaState.PLAY.getCode(),
            CurrentMediaState.PAUSE.getCode(),
            CurrentMediaState.STOP.getCode(),
            CurrentMediaState.LOADING.getCode(),
            CurrentMediaState.INTERRUPTED.getCode()
        );
    }
} 