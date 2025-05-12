package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Charging State Characteristic.
 * This characteristic represents the charging state of a battery.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/ChargingState">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "0000008F-0000-1000-8000-0026BB765291", name = "Charging State", tag = "chargingState")
@NonNullByDefault
public class HomekitChargingStateCharacteristic extends HomekitEnumCharacteristic {
    public enum ChargingState {
        NOT_CHARGING(0),
        CHARGING(1),
        NOT_CHARGEABLE(2);
        private final int code;
        ChargingState(int code) { this.code = code; }
        public int getCode() { return code; }
        public static ChargingState fromCode(int code) {
            for (ChargingState s : values()) {
                if (s.code == code) return s;
            }
            return NOT_CHARGING;
        }
    }
    public HomekitChargingStateCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, ChargingState.values().length);
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(true)
            .withDescription("Charging State");
    }
    public HomekitChargingStateCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }
    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && (value == ChargingState.NOT_CHARGING.getCode() || value == ChargingState.CHARGING.getCode() || value == ChargingState.NOT_CHARGEABLE.getCode());
    }
    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(ChargingState.NOT_CHARGING.getCode(), ChargingState.CHARGING.getCode(), ChargingState.NOT_CHARGEABLE.getCode());
    }
} 