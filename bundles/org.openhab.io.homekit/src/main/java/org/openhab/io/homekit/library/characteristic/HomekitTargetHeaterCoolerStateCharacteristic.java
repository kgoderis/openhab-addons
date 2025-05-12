package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Target Heater Cooler State Characteristic.
 * This characteristic represents the target state for a heater/cooler.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/TargetHeaterCoolerState">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "000000B2-0000-1000-8000-0026BB765291", name = "Target Heater Cooler State", tag = "targetHeaterCoolerState")
@NonNullByDefault
public class HomekitTargetHeaterCoolerStateCharacteristic extends HomekitEnumCharacteristic {
    public enum TargetHeaterCoolerState {
        AUTO(0),
        HEAT(1),
        COOL(2);
        private final int code;
        TargetHeaterCoolerState(int code) { this.code = code; }
        public int getCode() { return code; }
        public static TargetHeaterCoolerState fromCode(int code) {
            for (TargetHeaterCoolerState s : values()) {
                if (s.code == code) return s;
            }
            return AUTO;
        }
    }
    public HomekitTargetHeaterCoolerStateCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, TargetHeaterCoolerState.values().length);
        withInstanceId(instanceId)
            .withPairedWrite(true)
            .withPairedRead(true)
            .withEvents(true)
            .withDescription("Target Heater Cooler State");
    }
    public HomekitTargetHeaterCoolerStateCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }
    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= 0 && value < TargetHeaterCoolerState.values().length;
    }
    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(
            TargetHeaterCoolerState.AUTO.getCode(),
            TargetHeaterCoolerState.HEAT.getCode(),
            TargetHeaterCoolerState.COOL.getCode()
        );
    }
} 