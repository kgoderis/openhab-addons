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
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/SleepDiscoveryMode">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "000000E8-0000-1000-8000-0026BB765291", name = "Sleep Discovery Mode", tag = "sleepDiscoveryMode")
@NonNullByDefault
public class HomekitSleepDiscoveryModeCharacteristic extends HomekitEnumCharacteristic {
    public enum SleepDiscoveryMode {
        NOT_DISCOVERABLE(0),
        ALWAYS_DISCOVERABLE(1);
        private final int code;
        SleepDiscoveryMode(int code) { this.code = code; }
        public int getCode() { return code; }
        public static SleepDiscoveryMode fromCode(int code) {
            for (SleepDiscoveryMode s : values()) {
                if (s.code == code) return s;
            }
            return NOT_DISCOVERABLE;
        }
    }
    public HomekitSleepDiscoveryModeCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, SleepDiscoveryMode.values().length);
        withInstanceId(instanceId)
            .withPairedWrite(true)
            .withPairedRead(true)
            .withEvents(true)
            .withDescription("Sleep Discovery Mode");
    }
    public HomekitSleepDiscoveryModeCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }
    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= 0 && value < SleepDiscoveryMode.values().length;
    }
    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(
            SleepDiscoveryMode.NOT_DISCOVERABLE.getCode(),
            SleepDiscoveryMode.ALWAYS_DISCOVERABLE.getCode()
        );
    }
} 