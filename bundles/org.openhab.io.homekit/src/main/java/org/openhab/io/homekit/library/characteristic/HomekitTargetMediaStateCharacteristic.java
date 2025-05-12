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
 * The state can be one of: PLAY, PAUSE, STOP, or FAST_FORWARD.
 *
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "00000137-0000-1000-8000-0026BB765291", name = "Target Media State", tag = "targetMediaState")
@NonNullByDefault
public class HomekitTargetMediaStateCharacteristic extends HomekitEnumCharacteristic {
    public enum TargetMediaState {
        PLAY(0),
        PAUSE(1),
        STOP(2),
        FAST_FORWARD(3);

        private final int value;

        TargetMediaState(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static TargetMediaState fromValue(int value) {
            for (TargetMediaState state : values()) {
                if (state.value == value) {
                    return state;
                }
            }
            throw new IllegalArgumentException("Invalid Target Media State value: " + value);
        }
    }

    public HomekitTargetMediaStateCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, TargetMediaState.values().length);
        withInstanceId(instanceId)
            .withPairedRead(true)
            .withPairedWrite(true)
            .withEvents(true)
            .withDescription("Target Media State");
    }

    public HomekitTargetMediaStateCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= TargetMediaState.PLAY.getValue() 
            && value <= TargetMediaState.FAST_FORWARD.getValue();
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(
            TargetMediaState.PLAY.getValue(),
            TargetMediaState.PAUSE.getValue(),
            TargetMediaState.STOP.getValue(),
            TargetMediaState.FAST_FORWARD.getValue()
        );
    }

    public void setValue(TargetMediaState value) {
        try {
            setValue(value.getValue());
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to set Target Media State value", e);
        }
    }
} 