package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Current Heating Cooling State Characteristic.
 * This characteristic represents the current state of a heating/cooling system.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/CurrentHeatingCoolingState">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "0000000F-0000-1000-8000-0026BB765291", name = "Current Heating Cooling State", tag = "currentHeatingCoolingState")
@NonNullByDefault
public class HomekitCurrentHeatingCoolingStateCharacteristic extends HomekitEnumCharacteristic {
    public enum HeatingCoolingState {
        OFF(0),
        HEAT(1),
        COOL(2),
        AUTO(3);
        
        private final int code;
        
        HeatingCoolingState(int code) {
            this.code = code;
        }
        
        public int getCode() {
            return code;
        }
        
        public static HeatingCoolingState fromCode(int code) {
            for (HeatingCoolingState s : values()) {
                if (s.code == code) {
                    return s;
                }
            }
            return OFF;
        }
    }

    public HomekitCurrentHeatingCoolingStateCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, HeatingCoolingState.values().length);
        withInstanceId(instanceId)
            .withPairedWrite(false)
            .withPairedRead(true)
            .withEvents(true)
            .withDescription("Current Heating Cooling State");
    }

    public HomekitCurrentHeatingCoolingStateCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= 0 && value < HeatingCoolingState.values().length;
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(
            HeatingCoolingState.OFF.getCode(),
            HeatingCoolingState.HEAT.getCode(),
            HeatingCoolingState.COOL.getCode(),
            HeatingCoolingState.AUTO.getCode()
        );
    }
}
