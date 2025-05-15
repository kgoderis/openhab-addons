package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Position State Characteristic.
 * This characteristic represents the movement state of a door, window, or window covering.
 * The state can be one of: DECREASING, INCREASING, or STOPPED.
 *
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "00000072-0000-1000-8000-0026BB765291", name = "Position State", tag = "positionState", acceptedItemTypes = {
        "Number", "String" })
@NonNullByDefault
public class HomekitPositionStateCharacteristic extends HomekitEnumCharacteristic {
    public enum PositionState {
        DECREASING(0),
        INCREASING(1),
        STOPPED(2);

        private final int value;

        PositionState(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static PositionState fromValue(int value) {
            for (PositionState state : values()) {
                if (state.value == value) {
                    return state;
                }
            }
            throw new IllegalArgumentException("Invalid Position State value: " + value);
        }
    }

    public HomekitPositionStateCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, PositionState.values().length);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(false).withEvents(true)
                .withDescription("Position State");
    }

    public HomekitPositionStateCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public JsonObject toEventJson(Integer value) {
        return super.toEventJson(value);
    }

    @Override
    public JsonObject toEventJson() {
        return super.toEventJson();
    }

    @Override
    public JsonValue toValueJson(@Nullable Integer value) {
        return super.toValueJson(value);
    }

    @Override
    public JsonObject toJson() {
        return super.toJson();
    }

    @Override
    public JsonObject toJson(boolean includeMeta, boolean includePermissions, boolean includeType,
            boolean includeEvent) {
        return super.toJson(includeMeta, includePermissions, includeType, includeEvent);
    }

    @Override
    public JsonObject toReducedJson() {
        return super.toReducedJson();
    }

    @Override
    public State toState(Integer value) {
        return super.toState(value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        if (value == null) {
            return false;
        }
        try {
            PositionState.fromValue(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(PositionState.DECREASING.getValue(), PositionState.INCREASING.getValue(),
                PositionState.STOPPED.getValue());
    }
}
