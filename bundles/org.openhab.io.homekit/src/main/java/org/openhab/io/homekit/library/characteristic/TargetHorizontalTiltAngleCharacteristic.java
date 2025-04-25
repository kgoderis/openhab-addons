package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.internal.characteristic.IntegerCharacteristic;

@NonNullByDefault
public class TargetHorizontalTiltAngleCharacteristic extends IntegerCharacteristic {

    private static final String TYPE = "0000007B-0000-1000-8000-0026BB765291";

    public TargetHorizontalTiltAngleCharacteristic(Service service, long instanceId) {
        super(service, instanceId, true, true, true, "Target horizontal tilt angle", -90, 90, "arcdegrees", TYPE);
    }

    public TargetHorizontalTiltAngleCharacteristic(Service service, JsonValue value) {
        super(service, value);
    }

    public static String getType() {
        return TYPE;
    }

    @Override
    public State toState(Integer value) {
        return super.toState(value);
    }

    public static String getTag() {
        return TargetHorizontalTiltAngleCharacteristic.class.getSimpleName().replace("Characteristic", "");
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
