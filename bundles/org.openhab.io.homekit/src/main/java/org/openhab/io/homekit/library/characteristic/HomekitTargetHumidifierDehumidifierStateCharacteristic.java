package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Target Humidifier Dehumidifier State Characteristic.
 * This characteristic represents the target state for a humidifier/dehumidifier.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/TargetHumidifierDehumidifierState">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "000000B4-0000-1000-8000-0026BB765291", name = "Target Humidifier Dehumidifier State", tag = "targetHumidifierDehumidifierState")
@NonNullByDefault
public class HomekitTargetHumidifierDehumidifierStateCharacteristic extends HomekitEnumCharacteristic {
    public enum TargetHumidifierDehumidifierState {
        HUMIDIFIER(0),
        DEHUMIDIFIER(1),
        AUTO(2);
        private final int code;
        TargetHumidifierDehumidifierState(int code) { this.code = code; }
        public int getCode() { return code; }
        public static TargetHumidifierDehumidifierState fromCode(int code) {
            for (TargetHumidifierDehumidifierState s : values()) {
                if (s.code == code) return s;
            }
            return AUTO;
        }
    }
    public HomekitTargetHumidifierDehumidifierStateCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, TargetHumidifierDehumidifierState.values().length);
        withInstanceId(instanceId)
            .withPairedWrite(true)
            .withPairedRead(true)
            .withEvents(true)
            .withDescription("Target Humidifier Dehumidifier State");
    }
    public HomekitTargetHumidifierDehumidifierStateCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }
    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= 0 && value < TargetHumidifierDehumidifierState.values().length;
    }
    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(
            TargetHumidifierDehumidifierState.HUMIDIFIER.getCode(),
            TargetHumidifierDehumidifierState.DEHUMIDIFIER.getCode(),
            TargetHumidifierDehumidifierState.AUTO.getCode()
        );
    }
} 