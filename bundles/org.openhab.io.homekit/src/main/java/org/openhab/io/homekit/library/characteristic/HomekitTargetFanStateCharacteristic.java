package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Target Fan State Characteristic.
 * This characteristic represents the target state for a fan.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/TargetFanState">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "000000BF-0000-1000-8000-0026BB765291", name = "Target Fan State", tag = "targetFanState")
@NonNullByDefault
public class HomekitTargetFanStateCharacteristic extends HomekitEnumCharacteristic {
    public enum TargetFanState {
        MANUAL(0),
        AUTO(1);
        private final int code;
        TargetFanState(int code) { this.code = code; }
        public int getCode() { return code; }
        public static TargetFanState fromCode(int code) {
            for (TargetFanState s : values()) {
                if (s.code == code) return s;
            }
            return MANUAL;
        }
    }
    public HomekitTargetFanStateCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, TargetFanState.values().length);
        withInstanceId(instanceId)
            .withPairedWrite(true)
            .withPairedRead(true)
            .withEvents(true)
            .withDescription("Target Fan State");
    }
    public HomekitTargetFanStateCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }
    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= 0 && value < TargetFanState.values().length;
    }
    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(
            TargetFanState.MANUAL.getCode(),
            TargetFanState.AUTO.getCode()
        );
    }
} 