package org.openhab.io.homekit.library.characteristic;

import java.util.Set;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Managed Network Enable Characteristic.
 * This characteristic represents whether the managed network is enabled or disabled.
 *
 * @see <a href=\"https://developer.apple.com/documentation/HomeKit\">HAP Specification</a>
 * @author Karel Goderis - Initial Contribution
 */
@HomekitCharacteristicType(type = "00000215-0000-1000-8000-0026BB765291", name = "Managed Network Enable", tag = "managedNetworkEnable", acceptedItemTypes = {
        "Number" })
@NonNullByDefault
public class HomekitManagedNetworkEnableCharacteristic extends HomekitIntegerCharacteristic {
    /**
     * Enum representing the possible states for managed network enable.
     */
    public enum ManagedNetworkState {
        DISABLED(0),
        ENABLED(1);

        private final int code;

        ManagedNetworkState(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static ManagedNetworkState fromCode(int code) {
            for (ManagedNetworkState s : values()) {
                if (s.code == code)
                    return s;
            }
            return DISABLED;
        }
    }

    public HomekitManagedNetworkEnableCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, 0, 1, "");
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(true).withEvents(true).withTimedWrite(true)
                .withDescription("Managed Network Enable");
    }

    public HomekitManagedNetworkEnableCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    /**
     * Checks if the given value is a valid managed network state.
     *
     * @param value the value to check
     * @return true if the value is DISABLED or ENABLED, false otherwise
     */
    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null
                && (value == ManagedNetworkState.DISABLED.getCode() || value == ManagedNetworkState.ENABLED.getCode());
    }

    /**
     * Returns the set of allowed managed network state values.
     *
     * @return a set containing DISABLED and ENABLED
     */
    @Override
    public Set<Integer> getAllowedValues() {
        return Set.of(ManagedNetworkState.DISABLED.getCode(), ManagedNetworkState.ENABLED.getCode());
    }
}
