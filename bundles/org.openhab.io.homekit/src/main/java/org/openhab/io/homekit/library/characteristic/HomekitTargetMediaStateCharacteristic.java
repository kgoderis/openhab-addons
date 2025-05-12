package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Target Media State Characteristic.
 * This characteristic represents the target state for a media device.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/TargetMediaState">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "00000137-0000-1000-8000-0026BB765291", name = "Target Media State", tag = "targetMediaState")
@NonNullByDefault
public class HomekitTargetMediaStateCharacteristic extends HomekitEnumCharacteristic {
    public enum TargetMediaState {
        PLAY(0),
        PAUSE(1),
        STOP(2);
        private final int code;
        TargetMediaState(int code) { this.code = code; }
        public int getCode() { return code; }
        public static TargetMediaState fromCode(int code) {
            for (TargetMediaState s : values()) {
                if (s.code == code) return s;
            }
            return STOP;
        }
    }
    public HomekitTargetMediaStateCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, TargetMediaState.values().length);
        withInstanceId(instanceId)
            .withPairedWrite(true)
            .withPairedRead(true)
            .withEvents(true)
            .withDescription("Target Media State");
    }
    public HomekitTargetMediaStateCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }
    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= 0 && value < TargetMediaState.values().length;
    }
    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(
            TargetMediaState.PLAY.getCode(),
            TargetMediaState.PAUSE.getCode(),
            TargetMediaState.STOP.getCode()
        );
    }
} 