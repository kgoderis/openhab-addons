package org.openhab.io.homekit.internal.characteristic;

import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.types.State;
import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.ManagedService;

public abstract class FloatCharacteristic extends AbstractManagedCharacteristic<Double> {

    private final double minValue;
    private final double maxValue;
    private final double minStep;
    private final String unit;

    public FloatCharacteristic(HomekitCommunicationManager manager, ManagedService service, long instanceId,
            boolean isWritable, boolean isReadable, boolean hasEvents, String description, double minValue,
            double maxValue, double minStep, String unit) {
        super(manager, service, instanceId, "float", isWritable, isReadable, hasEvents, description);
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.minStep = minStep;
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
        base = enrich(base, "minStep", minStep);
        return enrich(base, "unit", unit);
    }

    @Override
    public JsonObject toReducedJson() {
        JsonObject base = super.toReducedJson();
        base = enrich(base, "minValue", minValue);
        base = enrich(base, "maxValue", maxValue);
        base = enrich(base, "minStep", minStep);
        return enrich(base, "unit", unit);
    }

    @Override
    public JsonObject toJson(boolean includeMeta, boolean includePermissions, boolean includeType,
            boolean includeEvent) {
        JsonObject base = toJson(true, true, includeType, includeType, includePermissions, includeMeta, includeEvent,
                false, true);
        base = enrich(base, "minValue", minValue);
        base = enrich(base, "maxValue", maxValue);
        base = enrich(base, "minStep", 1);
        return enrich(base, "unit", unit);
    }

    @Override
    protected Double convert(JsonValue value) {
        return ((JsonNumber) value).doubleValue();
    }

    @Override
    protected Double convert(State state) {
        DecimalType convertedState = state.as(DecimalType.class);
        if (convertedState == null) {
            return null;
        }

        return convertedState.doubleValue();
    }

    @Override
    protected State convert(Double value) {
        return new DecimalType(value);
    }

    @Override
    protected Double getDefault() {
        return minValue;
    }

    public static String getAcceptedItemType() {
        return CoreItemFactory.NUMBER;
    }

}
