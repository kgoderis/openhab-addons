package org.openhab.io.homekit.internal.characteristic;

import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.hap.Service;

@NonNullByDefault
public abstract class ByteCharacteristic extends GenericCharacteristic<Byte> {

    private final byte minValue;
    private final byte maxValue;
    private final String unit;

    public ByteCharacteristic(Service service, long instanceId, boolean isWritable, boolean isReadable,
            boolean hasEvents, String description, byte minValue, byte maxValue, String type) {
        super(service, instanceId, "uint8", isWritable, isReadable, hasEvents, description, type);
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.unit = "";
        initializeValue();
    }

    public ByteCharacteristic(Service service, JsonValue value) {
        super(service, value);
        JsonObject jsonObject = (JsonObject) value;
        this.minValue = jsonObject.containsKey("minValue") ? (byte) jsonObject.getInt("minValue") : 0;
        this.maxValue = jsonObject.containsKey("maxValue") ? (byte) jsonObject.getInt("maxValue") : Byte.MAX_VALUE;
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
        return enrich(base, "minStep", 1);
    }

    @Override
    public JsonObject toReducedJson() {
        JsonObject base = super.toReducedJson();
        base = enrich(base, "minValue", minValue);
        base = enrich(base, "maxValue", maxValue);
        return enrich(base, "minStep", 1);
    }

    @Override
    public JsonObject toJson(boolean includeMeta, boolean includePermissions, boolean includeType,
            boolean includeEvent) {
        JsonObject base = super.toJson(includeMeta, includePermissions, includeType, includeEvent);
        base = enrich(base, "minValue", minValue);
        base = enrich(base, "maxValue", maxValue);
        return enrich(base, "minStep", 1);
    }

    @Override
    public Byte toValue(JsonValue value) {
        return (byte) ((JsonNumber) value).intValue();
    }

    @Override
    public Byte toValue(State state) {
        DecimalType convertedState = state.as(DecimalType.class);
        if (convertedState == null) {
            return null;
        }
        return (byte) convertedState.intValue();
    }

    @Override
    public State toState(Byte value) {
        return new DecimalType(value);
    }

    @Override
    public Byte getDefault() {
        return minValue;
    }

    @Override
    public JsonObject toEventJson(Byte value) {
        return super.toEventJson(value);
    }

    @Override
    public JsonObject toEventJson() {
        return super.toEventJson();
    }

    @Override
    public JsonValue toValueJson(Byte value) {
        return super.toValueJson(value);
    }

    public static String getAcceptedItemType() {
        return CoreItemFactory.NUMBER;
    }
}
