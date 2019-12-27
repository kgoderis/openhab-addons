package org.openhab.io.homekit.internal.characteristic;

import javax.json.JsonNumber;
import javax.json.JsonObject;

import org.openhab.core.library.types.DecimalType;
import org.openhab.core.types.State;
import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.Service;

public abstract class FloatCharacteristic extends AbstractCharacteristic<Double> {

    private final double minValue;
    private final double maxValue;
    private final double minStep;
    private final String unit;

    public FloatCharacteristic(HomekitCommunicationManager manager, Service service, long instanceId,
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
    protected Double convert(JsonObject jsonObject) {
        return ((JsonNumber) jsonObject).doubleValue();
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

}
