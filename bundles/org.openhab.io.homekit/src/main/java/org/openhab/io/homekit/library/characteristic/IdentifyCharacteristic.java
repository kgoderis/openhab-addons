package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.internal.characteristic.WriteOnlyBooleanCharacteristic;

@NonNullByDefault
public class IdentifyCharacteristic extends WriteOnlyBooleanCharacteristic {
    private static final String TYPE = "00000014-0000-1000-8000-0026BB765291";

    public IdentifyCharacteristic(Service service, long instanceId) {
        super(service, instanceId, "Identify", TYPE);
    }

    public IdentifyCharacteristic(Service service, JsonValue value) {
        super(service, value);
    }

    public static String getType() {
        return TYPE;
    }

    @Override
    public void setValue(@Nullable Boolean value) {
        if (value != null && value) {
            getService().getAccessory().identify();
        }
    }

    /** {@inheritDoc} */
    @Override
    public Boolean getDefault() {
        return false;
    }

    public static String getTag() {
        return IdentifyCharacteristic.class.getSimpleName().replace("Characteristic", "");
    }

    @Override
    public JsonObject toEventJson(Boolean value) {
        return super.toEventJson(value);
    }

    @Override
    public JsonObject toEventJson() {
        return super.toEventJson();
    }

    @Override
    public JsonValue toValueJson(@Nullable Boolean value) {
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
    public State toState(Boolean value) {
        return super.toState(value);
    }
}
