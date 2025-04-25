package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.internal.characteristic.IntegerCharacteristic;

@NonNullByDefault
public class CurrentPositionCharacteristic extends IntegerCharacteristic {
    private static final String TYPE = "0000006D-0000-1000-8000-0026BB765291";

    public CurrentPositionCharacteristic(Service service, long instanceId) {
        super(service, instanceId, false, true, true, "Current position", 0, 100, "percent", TYPE);
    }

    public CurrentPositionCharacteristic(Service service, JsonValue value) {
        super(service, value);
    }

    public static String getType() {
        return TYPE;
    }

    public static String getTag() {
        return CurrentPositionCharacteristic.class.getSimpleName().replace("Characteristic", "");
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
}
