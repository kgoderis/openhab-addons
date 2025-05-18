package org.openhab.io.homekit.core.characteristic;

import java.util.Map;
import java.util.Set;

import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

@NonNullByDefault
public abstract class HomekitFloatCharacteristic extends AbstractHomekitCharacteristic<Double> {

    protected final double minValue;
    private final double maxValue;
    private final double minStep;
    private final String unit;

    public HomekitFloatCharacteristic(HomekitService service, HomekitEventManager eventManager, double minValue,
            double maxValue, double minStep, String unit) {
        super(service, eventManager);
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.minStep = minStep;
        this.unit = unit;
        withFormat("float").withPairedWrite(true).withPairedRead(true).withEvents(true);
        initializeValue();
    }

    public HomekitFloatCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
        JsonObject jsonObject = (JsonObject) value;
        this.minValue = jsonObject.containsKey("minValue") ? jsonObject.getJsonNumber("minValue").doubleValue() : 0;
        this.maxValue = jsonObject.containsKey("maxValue") ? jsonObject.getJsonNumber("maxValue").doubleValue() : 100;
        this.minStep = jsonObject.containsKey("minStep") ? jsonObject.getJsonNumber("minStep").doubleValue() : 1;
        this.unit = jsonObject.containsKey("unit") ? jsonObject.getString("unit") : "";
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
        JsonObject base = super.toJson(includeMeta, includePermissions, includeType, includeEvent);
        base = enrich(base, "minValue", minValue);
        base = enrich(base, "maxValue", maxValue);
        base = enrich(base, "minStep", minStep);
        return enrich(base, "unit", unit);
    }

    @Override
    public Double toValue(JsonValue value, Map<String, Object> conversionMap) {
        return ((JsonNumber) value).doubleValue();
    }

    @Override
    public Double toValue(State state, Map<String, Object> conversionMap) {
        DecimalType convertedState = state.as(DecimalType.class);
        if (convertedState == null) {
            return minValue;
        }
        return convertedState.doubleValue();
    }

    @Override
    public State toState(Double value) {
        return new DecimalType(value);
    }

    @Override
    public Double getDefault() {
        return minValue;
    }

    public static String getAcceptedItemType() {
        return CoreItemFactory.NUMBER;
    }

    @Override
    public boolean isAllowedValue(Double value) {
        return value != null && value >= minValue && value <= maxValue;
    }

    @Override
    public Set<Double> getAllowedValues() {
        return java.util.Collections.emptySet(); // No specific allowed values, just a range
    }
}
