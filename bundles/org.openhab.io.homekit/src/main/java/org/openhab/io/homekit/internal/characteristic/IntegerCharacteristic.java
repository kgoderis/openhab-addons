package org.openhab.io.homekit.internal.characteristic;

import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.types.State;
import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.ManagedService;

public abstract class IntegerCharacteristic extends AbstractManagedCharacteristic<Integer> {

    private final int minValue;
    private final int maxValue;
    private final String unit;

    public IntegerCharacteristic(HomekitCommunicationManager manager, ManagedService service, long instanceId,
            boolean isWritable, boolean isReadable, boolean hasEvents, String description, int minValue, int maxValue,
            String unit) {
        super(manager, service, instanceId, "int", isWritable, isReadable, hasEvents, description);
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
        JsonObject base = toJson(true, true, includeType, includeType, includePermissions, includeMeta, includeEvent,
                false, true);
        base = enrich(base, "minValue", minValue);
        base = enrich(base, "maxValue", maxValue);
        base = enrich(base, "minStep", 1);
        return enrich(base, "unit", unit);
    }

    @Override
    protected Integer convert(JsonValue value) {
        return ((JsonNumber) value).intValue();
    }

    @Override
    protected Integer convert(State state) {
        DecimalType convertedState = state.as(DecimalType.class);
        if (convertedState == null) {
            return null;
        }

        return convertedState.intValue();
    }

    @Override
    protected State convert(Integer value) {
        return new DecimalType(value);
    }

    @Override
    protected Integer getDefault() {
        return minValue;
    }

    public static String getAcceptedItemType() {
        return CoreItemFactory.NUMBER;
    }
}
