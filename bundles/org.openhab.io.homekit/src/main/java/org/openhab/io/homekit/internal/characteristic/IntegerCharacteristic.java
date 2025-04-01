package org.openhab.io.homekit.internal.characteristic;

import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.Service;

public abstract class IntegerCharacteristic extends GenericCharacteristic<Integer> {

    private final int minValue;
    private final int maxValue;
    private final String unit;

    public IntegerCharacteristic(Service service, long instanceId, boolean isWritable, boolean isReadable,
            boolean hasEvents, String description, int minValue, int maxValue, String unit) {
        super(service, instanceId, "int", isWritable, isReadable, hasEvents, description);
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.unit = unit;
    }

    @Override
    public boolean isHidden() {
        return false;
    }

    @Override
    public JsonObject toJson() {
        JsonObject base = super.toJson();
        base = enrich(base, "minValue", minValue);
        base = enrich(base, "maxValue", maxValue);
        base = enrich(base, "minStep", 1);
        return enrich(base, "unit", unit);
    }

    @Override
    public JsonObject toReducedJson() {
        JsonObject base = super.toReducedJson();
        base = enrich(base, "minValue", minValue);
        base = enrich(base, "maxValue", maxValue);
        base = enrich(base, "minStep", 1);
        return enrich(base, "unit", unit);
    }

    @Override
    public JsonObject toJson(boolean includeMeta, boolean includePermissions, boolean includeType,
            boolean includeEvent) {
        JsonObject base = super.toJson(includeMeta, includePermissions, includeType, includeEvent);
        base = enrich(base, "minValue", minValue);
        base = enrich(base, "maxValue", maxValue);
        base = enrich(base, "minStep", 1);
        return enrich(base, "unit", unit);
    }

    @Override
    public Integer toValue(JsonValue value) {
        return ((JsonNumber) value).intValue();
    }

    @Override
    public Integer toValue(State state) {
        DecimalType convertedState = state.as(DecimalType.class);
        if (convertedState == null) {
            return null;
        }
        return convertedState.intValue();
    }

    @Override
    public State toState(Integer value) {
        return new DecimalType(value);
    }

    @Override
    public Integer getDefault() {
        return minValue;
    }

    public static String getAcceptedItemType() {
        return CoreItemFactory.NUMBER;
    }
}
