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
 *
 * @see <a href="https://developer.apple.com/documentation/homekit/hmcharacteristicpositionstate">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "00000072-0000-1000-8000-0026BB765291", name = "Position State", tag = "positionState")
@NonNullByDefault
public class HomekitPositionStateCharacteristic extends HomekitEnumCharacteristic {

    public enum PositionState {
        DECREASING(0),
        INCREASING(1),
        STOPPED(2);
        private final int code;
        PositionState(int code) { this.code = code; }
        public int getCode() { return code; }
        public static PositionState fromCode(int code) {
            for (PositionState v : values()) {
                if (v.code == code) return v;
            }
            return STOPPED;
        }
    }

    public HomekitPositionStateCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, PositionState.values().length);
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(true)
            .withDescription("Position State");
    }

    public HomekitPositionStateCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
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
        return value != null && (value == PositionState.DECREASING.getCode() || 
                                value == PositionState.INCREASING.getCode() ||
                                value == PositionState.STOPPED.getCode());
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(PositionState.DECREASING.getCode(), 
                              PositionState.INCREASING.getCode(),
                              PositionState.STOPPED.getCode());
    }
}
