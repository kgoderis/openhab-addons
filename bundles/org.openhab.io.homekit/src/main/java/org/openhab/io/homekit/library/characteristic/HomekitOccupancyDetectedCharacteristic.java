package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * Occupancy Detected characteristic.
 * This characteristic represents whether occupancy has been detected in a room or area.
 * The value is an enumeration with two states: NOT_DETECTED and DETECTED.
 *
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 * @author Karel Goderis - Initial contribution
 */
@HomekitCharacteristicType(type = "00000071-0000-1000-8000-0026BB765291", name = "Occupancy Detected", tag = "occupancyDetected", acceptedItemTypes = {
        "Number", "String" })
@NonNullByDefault
public class HomekitOccupancyDetectedCharacteristic extends HomekitEnumCharacteristic {

    public enum OccupancyDetected {
        NOT_DETECTED(0),
        DETECTED(1);

        private final int code;

        OccupancyDetected(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static OccupancyDetected fromCode(int code) {
            for (OccupancyDetected state : values()) {
                if (state.code == code) {
                    return state;
                }
            }
            return NOT_DETECTED;
        }
    }

    /**
     * Creates a new Occupancy Detected characteristic.
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param instanceId The instance ID for this characteristic
     */
    public HomekitOccupancyDetectedCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, OccupancyDetected.values().length);
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(true)
                .withDescription("Occupancy Detected");
    }

    /**
     * Creates a new Occupancy Detected characteristic from a JSON value.
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param value The JSON value to initialize the characteristic with
     */
    public HomekitOccupancyDetectedCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null
                && (value == OccupancyDetected.NOT_DETECTED.getCode() || value == OccupancyDetected.DETECTED.getCode());
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(OccupancyDetected.NOT_DETECTED.getCode(), OccupancyDetected.DETECTED.getCode());
    }
}
