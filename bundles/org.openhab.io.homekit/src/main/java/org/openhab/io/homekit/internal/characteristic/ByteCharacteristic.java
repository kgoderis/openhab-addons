package org.openhab.io.homekit.internal.characteristic;

import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.Service;

public abstract class ByteCharacteristic extends GenericCharacteristic<Byte> {

    private final byte minValue;
    private final byte maxValue;

    public ByteCharacteristic(Service service, long instanceId, boolean isWritable, boolean isReadable,
            boolean hasEvents, String description, byte minValue, byte maxValue) {
        super(service, instanceId, "uint8", isWritable, isReadable, hasEvents, description);
        this.minValue = minValue;
        this.maxValue = maxValue;
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

    public static String getAcceptedItemType() {
        return CoreItemFactory.NUMBER;
    }
}
