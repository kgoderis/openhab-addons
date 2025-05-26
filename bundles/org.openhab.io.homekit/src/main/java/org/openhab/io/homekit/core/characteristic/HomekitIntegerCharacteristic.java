package org.openhab.io.homekit.core.characteristic;

import java.util.Map;

import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

@NonNullByDefault
public abstract class HomekitIntegerCharacteristic extends AbstractHomekitCharacteristic<Integer> {

    protected final int minValue;
    private final int maxValue;
    private final String unit;

    public HomekitIntegerCharacteristic(HomekitService service, HomekitEventManager eventManager, int minValue,
            int maxValue, String unit) {
        super(service, eventManager);
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.unit = unit;
        withFormat("int").withPairedWrite(true).withPairedRead(true).withEvents(true);
        initializeValue();
    }

    public HomekitIntegerCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
        JsonObject jsonObject = (JsonObject) value;
        this.minValue = jsonObject.containsKey("minValue") ? jsonObject.getInt("minValue") : 0;
        this.maxValue = jsonObject.containsKey("maxValue") ? jsonObject.getInt("maxValue") : Integer.MAX_VALUE;
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
    public Integer toValue(JsonValue value, Map<String, Object> conversionMap) {
        return ((JsonNumber) value).intValue();
    }

    @Override
    public Integer toValue(State state, Map<String, Object> conversionMap) {
        DecimalType convertedState = state.as(DecimalType.class);
        if (convertedState == null) {
            return minValue;
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
