package org.openhab.io.homekit.internal.characteristic;

import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.hap.HomekitService;
import org.openhab.io.homekit.internal.events.HomekitEventManager;

@NonNullByDefault
public abstract class HomekitLongCharacteristic extends HomekitBaseCharacteristic<Long> {

    private final long minValue;
    private final long maxValue;
    private final long minStep;

    public HomekitLongCharacteristic(HomekitService service, long instanceId, boolean isWritable, boolean isReadable,
            boolean hasEvents, String description, long minValue, long maxValue, long minStep, String type, HomekitEventManager eventManager) {
        super(service, instanceId, "uint32", isWritable, isReadable, hasEvents, description, type, eventManager);
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.minStep = minStep;
        initializeValue();
    }

    public HomekitLongCharacteristic(HomekitService service, JsonValue value, HomekitEventManager eventManager) {
        super(service, value, eventManager);
        JsonObject jsonObject = (JsonObject) value;
        this.minValue = jsonObject.containsKey("minValue") ? jsonObject.getJsonNumber("minValue").longValue() : 0;
        this.maxValue = jsonObject.containsKey("maxValue") ? jsonObject.getJsonNumber("maxValue").longValue()
                : Long.MAX_VALUE;
        this.minStep = jsonObject.containsKey("minStep") ? jsonObject.getJsonNumber("minStep").longValue() : 1;
        initializeValue();
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
        JsonObject base = super.toJson(includeMeta, includePermissions, includeType, includeEvent);
        base = enrich(base, "minValue", minValue);
        base = enrich(base, "maxValue", maxValue);
        return enrich(base, "minStep", minStep);
    }

    @Override
    public JsonObject toEventJson() {
        return super.toEventJson();
    }

    @Override
    public JsonObject toEventJson(Long value) {
        return super.toEventJson(value);
    }

    @Override
    public JsonValue toValueJson(@Nullable Long value) {
        return super.toValueJson(value);
    }

    @Override
    public Long toValue(JsonValue value) {
        return ((JsonNumber) value).longValue();
    }

    @Override
    public Long toValue(State state) {
        DecimalType convertedState = state.as(DecimalType.class);
        if (convertedState == null) {
            return minValue;
        }
        return convertedState.longValue();
    }

    @Override
    public State toState(Long value) {
        return new DecimalType(value);
    }

    @Override
    public Long getDefault() {
        return minValue;
    }

    public static String getAcceptedItemType() {
        return CoreItemFactory.NUMBER;
    }
}
