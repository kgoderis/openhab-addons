package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.internal.characteristic.LongCharacteristic;

@NonNullByDefault
public class ColorTemperatureCharacteristic extends LongCharacteristic {
    private static final String TYPE = "000000CE-0000-1000-8000-0026BB765291";

    public ColorTemperatureCharacteristic(Service service, long instanceId) {
        super(service, instanceId, true, true, true, "Color temperature", 50L, 400L, 1L, TYPE);
    }

    public ColorTemperatureCharacteristic(Service service, JsonValue value) {
        super(service, value);
    }

    public static String getType() {
        return TYPE;
    }

    public static String getTag() {
        return ColorTemperatureCharacteristic.class.getSimpleName().replace("Characteristic", "");
    }

    @Override
    public JsonObject toEventJson(Long value) {
        return super.toEventJson(value);
    }

    @Override
    public JsonObject toEventJson() {
        return super.toEventJson();
    }

    @Override
    public JsonValue toValueJson(@Nullable Long value) {
        return super.toValueJson(value);
    }

    @Override
    public State toState(Long value) {
        return super.toState(value);
    }

    @Override
    public JsonObject toJson(boolean includeMeta, boolean includePermissions, boolean includeType,
            boolean includeEvent) {
        return super.toJson(includeMeta, includePermissions, includeType, includeEvent);
    }

    @Override
    public JsonObject toJson() {
        return super.toJson();
    }

    @Override
    public JsonObject toReducedJson() {
        return super.toReducedJson();
    }
}
