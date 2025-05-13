package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import java.util.Set;
import java.util.HashSet;

/**
 * HomeKit Lock Physical Controls Characteristic.
 * <p>
 * This characteristic represents the physical controls state of a lock, such as whether the physical controls are enabled or disabled. The value is an integer corresponding to a specific state as defined by the HAP specification.
 * <p>
 * See the HomeKit Accessory Protocol (HAP) specification for details: https://developer.apple.com/documentation/HomeKit
 *
 * @author Karel Goderis
 */
@HomekitCharacteristicType(type = "000000A7-0000-1000-8000-0026BB765291", name = "Lock Physical Controls", tag = "lockPhysicalControls")
@NonNullByDefault
public class HomekitLockPhysicalControlsCharacteristic extends HomekitIntegerCharacteristic {

    /**
     * Enum representing the possible physical control states for a lock.
     */
    public enum LockPhysicalControls {
        DISABLED(0),
        ENABLED(1);
        private final int code;
        LockPhysicalControls(int code) { this.code = code; }
        public int getCode() { return code; }
        public static LockPhysicalControls fromCode(int code) {
            for (LockPhysicalControls s : values()) {
                if (s.code == code) return s;
            }
            return DISABLED;
        }
    }

    /**
     * Constructs a new Lock Physical Controls characteristic.
     *
     * @param service the HomeKit service this characteristic belongs to
     * @param eventManager the event manager for handling HomeKit events
     * @param instanceId the instance ID for this characteristic
     */
    public HomekitLockPhysicalControlsCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 0, 1, "");
        withInstanceId(instanceId)
            .withPairedWrite(true)
            .withPairedRead(true)
            .withEvents(true)
            .withDescription("Lock Physical Controls");
    }

    /**
     * Constructs a new Lock Physical Controls characteristic from a JSON value.
     *
     * @param service the HomeKit service this characteristic belongs to
     * @param eventManager the event manager for handling HomeKit events
     * @param value the JSON value to initialize the characteristic with
     */
    public HomekitLockPhysicalControlsCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    /**
     * Checks if the given value is an allowed lock physical controls state.
     *
     * @param value the value to check
     * @return true if the value is allowed, false otherwise
     */
    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && (value == 0 || value == 1);
    }

    /**
     * Returns the set of allowed lock physical controls state values.
     *
     * @return the set of allowed values
     */
    @Override
    public Set<Integer> getAllowedValues() {
        Set<Integer> set = new HashSet<>();
        for (LockPhysicalControls s : LockPhysicalControls.values()) {
            set.add(s.getCode());
        }
        return set;
    }
} 