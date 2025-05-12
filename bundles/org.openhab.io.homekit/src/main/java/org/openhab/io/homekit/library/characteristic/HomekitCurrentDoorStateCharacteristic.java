package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Current Door State Characteristic.
 * This characteristic represents the current state of a door.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/CurrentDoorState">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "0000000E-0000-1000-8000-0026BB765291", name = "Current Door State", tag = "currentDoorState")
@NonNullByDefault
public class HomekitCurrentDoorStateCharacteristic extends HomekitEnumCharacteristic {
    public enum DoorState {
        OPEN(0),
        CLOSED(1),
        OPENING(2),
        CLOSING(3),
        STOPPED(4);
        
        private final int code;
        
        DoorState(int code) {
            this.code = code;
        }
        
        public int getCode() {
            return code;
        }
        
        public static DoorState fromCode(int code) {
            for (DoorState s : values()) {
                if (s.code == code) {
                    return s;
                }
            }
            return STOPPED;
        }
    }

    public HomekitCurrentDoorStateCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, DoorState.values().length);
        withInstanceId(instanceId)
            .withPairedWrite(false)
            .withPairedRead(true)
            .withEvents(true)
            .withDescription("Current Door State");
    }

    public HomekitCurrentDoorStateCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= 0 && value < DoorState.values().length;
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(
            DoorState.OPEN.getCode(),
            DoorState.CLOSED.getCode(),
            DoorState.OPENING.getCode(),
            DoorState.CLOSING.getCode(),
            DoorState.STOPPED.getCode()
        );
    }
} 