package org.openhab.io.homekit.internal.characteristic;

import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.hap.Service;

/**
 * * Characteristic that exposes an Enum value. Enums are represented as an Integer value in the
 * Homekit protocol, and classes extending this one must handle the static mapping to an Integer
 * value.
 **/
@NonNullByDefault
public abstract class EnumCharacteristic extends GenericCharacteristic<Integer> {

    private final int maxValue;

    public EnumCharacteristic(Service service, long instanceId, boolean isWritable, boolean isReadable,
            boolean hasEvents, String description, int maxValue, String type) {
        super(service, instanceId, "int", isWritable, isReadable, hasEvents, description, type);
        this.maxValue = maxValue;
        initializeValue();
    }

    public EnumCharacteristic(Service service, JsonValue value) {
        super(service, value);
        JsonObject jsonObject = (JsonObject) value;
        this.maxValue = jsonObject.containsKey("maxValue") ? jsonObject.getInt("maxValue") : 1;
        initializeValue();
    }

    @Override
    public boolean isHidden() {
        return false;
    }

    @Override
    public JsonObject toJson() {
        JsonObject base = super.toJson();
        base = enrich(base, "minValue", 0);
        base = enrich(base, "maxValue", maxValue);
        return enrich(base, "minStep", 1);
    }

    @Override
    public JsonObject toReducedJson() {
        JsonObject base = super.toReducedJson();
        base = enrich(base, "minValue", 0);
        base = enrich(base, "maxValue", maxValue);
        return enrich(base, "minStep", 1);
    }

    @Override
    public JsonObject toJson(boolean includeMeta, boolean includePermissions, boolean includeType,
            boolean includeEvent) {
        JsonObject base = super.toJson(includeMeta, includePermissions, includeType, includeEvent);
        base = enrich(base, "minValue", 0);
        base = enrich(base, "maxValue", maxValue);
        return enrich(base, "minStep", 1);
    }

    @Override
    public Integer toValue(JsonValue value) {
        if (value instanceof JsonNumber jsonNumber) {
            return jsonNumber.intValue();
        } else if (value == JsonValue.TRUE) {
            return 1; // For at least one enum type (locks), homekit will send a true instead of 1
        } else if (value == JsonValue.FALSE) {
            return 0;
        } else {
            throw new IndexOutOfBoundsException("Cannot convert " + (value != null ? value.getClass() : "null") + " to Integer");
        }
    }

    @Override
    public Integer toValue(State state) {
        DecimalType convertedState = state.as(DecimalType.class);
        if (convertedState == null) {
            return 0;
        }
        return convertedState.intValue();
    }

    @Override
    public State toState(Integer value) {
        return new DecimalType(value);
    }

    @Override
    public Integer getDefault() {
        return 0;
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

    public static String getAcceptedItemType() {
        return CoreItemFactory.NUMBER;
    }
}
