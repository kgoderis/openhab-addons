package org.openhab.io.homekit.internal.characteristic;

import javax.json.JsonNumber;
import javax.json.JsonObject;

import org.openhab.core.library.types.DecimalType;
import org.openhab.core.types.State;
import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.Service;

public abstract class LongCharacteristic extends AbstractCharacteristic<Long> {

    private final long minValue;
    private final long maxValue;
    private final long minStep;

    public LongCharacteristic(HomekitCommunicationManager manager, Service service, long instanceId, boolean isWritable,
            boolean isReadable, boolean hasEvents, String description, long minValue, long maxValue, long minStep) {
        super(manager, service, instanceId, "uint32", isWritable, isReadable, hasEvents, description);
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.minStep = minStep;
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
        return enrich(base, "minStep", minStep);
    }

    @Override
    protected Long convert(JsonObject jsonObject) {
        return ((JsonNumber) jsonObject).longValue();
    }

    @Override
    protected Long convert(State state) {
        DecimalType convertedState = state.as(DecimalType.class);
        if (convertedState == null) {
            return null;
        }

        return convertedState.longValue();
    }

    @Override
    protected State convert(Long value) {
        return new DecimalType(value);
    }

    @Override
    protected Long getDefault() {
        return minValue;
    }

}
