package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Sleep Discovery Mode Characteristic.
 * This characteristic represents the sleep discovery mode for a device.
 * The mode can be one of: NOT_DISCOVERABLE (0), ALWAYS_DISCOVERABLE (1), or DISCOVERABLE_WHEN_SLEEPING (2).
 * This determines when the device is discoverable in the HomeKit network.
 *
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "000000E8-0000-1000-8000-0026BB765291", name = "Sleep Discovery Mode", tag = "sleepDiscoveryMode")
@NonNullByDefault
public class HomekitSleepDiscoveryModeCharacteristic extends HomekitEnumCharacteristic {
    public enum SleepDiscoveryMode {
        NOT_DISCOVERABLE(0),
        ALWAYS_DISCOVERABLE(1),
        DISCOVERABLE_WHEN_SLEEPING(2);

        private final int value;
        SleepDiscoveryMode(int value) { this.value = value; }
        public int getValue() { return value; }
        public static SleepDiscoveryMode fromValue(int value) {
            for (SleepDiscoveryMode mode : values()) {
                if (mode.value == value) {
                    return mode;
                }
            }
            throw new IllegalArgumentException("Invalid Sleep Discovery Mode value: " + value);
        }
    }

    public HomekitSleepDiscoveryModeCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 3);
        withInstanceId(instanceId)
            .withPairedRead(true)
            .withPairedWrite(true)
            .withEvents(true)
            .withDescription("Sleep Discovery Mode");
    }

    public HomekitSleepDiscoveryModeCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        if (value == null) return false;
        try {
            SleepDiscoveryMode.fromValue(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        java.util.Set<Integer> allowed = new java.util.HashSet<>();
        for (SleepDiscoveryMode mode : SleepDiscoveryMode.values()) {
            allowed.add(mode.getValue());
        }
        return allowed;
    }
} 