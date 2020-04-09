package org.openhab.io.homekit.internal.characteristic;

import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.types.State;
import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.ManagedService;

public abstract class LongCharacteristic extends AbstractManagedCharacteristic<Long> {

    private final long minValue;
    private final long maxValue;
    private final long minStep;

    public LongCharacteristic(HomekitCommunicationManager manager, ManagedService service, long instanceId,
            boolean isWritable, boolean isReadable, boolean hasEvents, String description, long minValue, long maxValue,
            long minStep) {
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
    public JsonObject toReducedJson() {
        JsonObject base = super.toReducedJson();
        base = enrich(base, "minValue", minValue);
        base = enrich(base, "maxValue", maxValue);
        return enrich(base, "minStep", minStep);
    }

    @Override
    public JsonObject toJson(boolean includeMeta, boolean includePermissions, boolean includeType,
            boolean includeEvent) {
        JsonObject base = toJson(true, true, includeType, includeType, includePermissions, includeMeta, includeEvent,
                false, true);
        base = enrich(base, "minValue", minValue);
        base = enrich(base, "maxValue", maxValue);
        return enrich(base, "minStep", minStep);
    }

    @Override
    protected Long convert(JsonValue value) {
        return ((JsonNumber) value).longValue();
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

    public static String getAcceptedItemType() {
        return CoreItemFactory.NUMBER;
    }

}
