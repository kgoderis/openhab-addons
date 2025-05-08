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
public abstract class HomekitByteCharacteristic extends HomekitGenericCharacteristic<Byte> {

    private final byte minValue;
    private final byte maxValue;

    public HomekitByteCharacteristic(HomekitService service, long instanceId, boolean isWritable, boolean isReadable,
            boolean hasEvents, String description, byte minValue, byte maxValue, String type, HomekitEventManager eventManager) {
        super(service, instanceId, "uint8", isWritable, isReadable, hasEvents, description, type, eventManager);
        this.minValue = minValue;
        this.maxValue = maxValue;
        initializeValue();
    }

    public HomekitByteCharacteristic(HomekitService service, JsonValue value, HomekitEventManager eventManager) {
        super(service, value, eventManager);
        JsonObject jsonObject = (JsonObject) value;
        this.minValue = jsonObject.containsKey("minValue") ? (byte) jsonObject.getInt("minValue") : 0;
        this.maxValue = jsonObject.containsKey("maxValue") ? (byte) jsonObject.getInt("maxValue") : Byte.MAX_VALUE;
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
            return minValue;
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
    public JsonValue toValueJson(@Nullable Byte value) {
        return super.toValueJson(value);
    }

    public static String getAcceptedItemType() {
        return CoreItemFactory.NUMBER;
    }
}
