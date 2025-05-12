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

@HomekitCharacteristicType(type = "00000033-0000-1000-8000-0026BB765291", name = "Target Heating Cooling State", tag = "targetHeatingCoolingState")
@NonNullByDefault
public class HomekitTargetHeatingCoolingStateCharacteristic extends HomekitEnumCharacteristic {

    public enum TargetHeatingCoolingState {
        OFF(0),
        HEAT(1),
        COOL(2),
        AUTO(3);
        private final int code;
        TargetHeatingCoolingState(int code) { this.code = code; }
        public int getCode() { return code; }
        public static TargetHeatingCoolingState fromCode(int code) {
            for (TargetHeatingCoolingState v : values()) {
                if (v.code == code) return v;
            }
            return OFF;
        }
    }

    public HomekitTargetHeatingCoolingStateCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, TargetHeatingCoolingState.values().length);
        withInstanceId(instanceId).withPairedWrite(true).withPairedRead(true).withEvents(true)
            .withDescription("Target Heating Cooling State");
    }

    public HomekitTargetHeatingCoolingStateCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= TargetHeatingCoolingState.OFF.getCode() && value <= TargetHeatingCoolingState.AUTO.getCode();
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(
            TargetHeatingCoolingState.OFF.getCode(),
            TargetHeatingCoolingState.HEAT.getCode(),
            TargetHeatingCoolingState.COOL.getCode(),
            TargetHeatingCoolingState.AUTO.getCode()
        );
    }

    @Override
    public State toState(Integer value) {
        return super.toState(value);
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
}
