package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Target Door State Characteristic.
 * This characteristic represents the target state for a door.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/TargetDoorState">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "00000032-0000-1000-8000-0026BB765291", name = "Target Door State", tag = "targetDoorState")
@NonNullByDefault
public class HomekitTargetDoorStateCharacteristic extends HomekitEnumCharacteristic {

    public enum TargetDoorState {
        OPEN(0),
        CLOSED(1);
        private final int code;
        TargetDoorState(int code) { this.code = code; }
        public int getCode() { return code; }
        public static TargetDoorState fromCode(int code) {
            for (TargetDoorState s : values()) {
                if (s.code == code) return s;
            }
            return CLOSED;
        }
    }

    public HomekitTargetDoorStateCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, TargetDoorState.values().length);
        withInstanceId(instanceId)
            .withPairedWrite(true)
            .withPairedRead(true)
            .withEvents(true)
            .withDescription("Target Door State");
    }

    public HomekitTargetDoorStateCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= 0 && value < TargetDoorState.values().length;
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(
            TargetDoorState.OPEN.getCode(),
            TargetDoorState.CLOSED.getCode()
        );
    }
} 