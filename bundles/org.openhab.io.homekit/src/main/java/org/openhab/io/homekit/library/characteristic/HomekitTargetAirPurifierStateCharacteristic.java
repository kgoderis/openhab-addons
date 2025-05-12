package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Target Air Purifier State Characteristic.
 * This characteristic represents the target state for an air purifier.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/TargetAirPurifierState">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "000000A8-0000-1000-8000-0026BB765291", name = "Target Air Purifier State", tag = "targetAirPurifierState")
@NonNullByDefault
public class HomekitTargetAirPurifierStateCharacteristic extends HomekitEnumCharacteristic {
    public enum TargetAirPurifierState {
        MANUAL(0),
        AUTO(1);
        private final int code;
        TargetAirPurifierState(int code) { this.code = code; }
        public int getCode() { return code; }
        public static TargetAirPurifierState fromCode(int code) {
            for (TargetAirPurifierState s : values()) {
                if (s.code == code) return s;
            }
            return MANUAL;
        }
    }
    public HomekitTargetAirPurifierStateCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, TargetAirPurifierState.values().length);
        withInstanceId(instanceId)
            .withPairedWrite(true)
            .withPairedRead(true)
            .withEvents(true)
            .withDescription("Target Air Purifier State");
    }
    public HomekitTargetAirPurifierStateCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }
    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= 0 && value < TargetAirPurifierState.values().length;
    }
    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(
            TargetAirPurifierState.MANUAL.getCode(),
            TargetAirPurifierState.AUTO.getCode()
        );
    }
} 