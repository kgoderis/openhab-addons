package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Current Air Purifier State Characteristic.
 * This characteristic represents the current state of an air purifier.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/CurrentAirPurifierState">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "000000A9-0000-1000-8000-0026BB765291", name = "Current Air Purifier State", tag = "currentAirPurifierState")
@NonNullByDefault
public class HomekitCurrentAirPurifierStateCharacteristic extends HomekitEnumCharacteristic {
    public enum AirPurifierState {
        INACTIVE(0),
        IDLE(1),
        PURIFYING_AIR(2);
        
        private final int code;
        
        AirPurifierState(int code) {
            this.code = code;
        }
        
        public int getCode() {
            return code;
        }
        
        public static AirPurifierState fromCode(int code) {
            for (AirPurifierState s : values()) {
                if (s.code == code) {
                    return s;
                }
            }
            return INACTIVE;
        }
    }

    public HomekitCurrentAirPurifierStateCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, AirPurifierState.values().length);
        withInstanceId(instanceId)
            .withPairedWrite(false)
            .withPairedRead(true)
            .withEvents(true)
            .withDescription("Current Air Purifier State");
    }

    public HomekitCurrentAirPurifierStateCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= 0 && value < AirPurifierState.values().length;
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(
            AirPurifierState.INACTIVE.getCode(),
            AirPurifierState.IDLE.getCode(),
            AirPurifierState.PURIFYING_AIR.getCode()
        );
    }
} 