package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.internal.characteristic.IntegerCharacteristic;

public class CurrentHorizontalTiltAngleCharacteristic extends IntegerCharacteristic {

    private static final String TYPE = "0000006C-0000-1000-8000-0026BB765291";

    public CurrentHorizontalTiltAngleCharacteristic(Service service, long instanceId) {
        super(service, instanceId, false, true, true, "Current horizontal tilt angle", -90, 90, "arcdegrees");
    }

    public CurrentHorizontalTiltAngleCharacteristic(Service service, JsonValue value) {
        super(service, value);
    }

    @Override
    public static String getType() {
        return TYPE;
    }

    @Override
    public String getInstanceType() {
        return TYPE;
    }

    public static String getTag() {
        return CurrentHorizontalTiltAngleCharacteristic.class.getSimpleName().replace("Characteristic", "");
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
    public JsonValue toValueJson(Integer value) {
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
}
