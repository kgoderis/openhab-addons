package org.openhab.io.homekit.internal.characteristic;

import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.openhab.core.library.types.DecimalType;
import org.openhab.core.types.State;
import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.Service;

/**
 * * Characteristic that exposes an Enum value. Enums are represented as an Integer value in the
 * Homekit protocol, and classes extending this one must handle the static mapping to an Integer
 * value.
 **/
public abstract class EnumCharacteristic extends AbstractCharacteristic<Integer> {

    private final int maxValue;

    public EnumCharacteristic(HomekitCommunicationManager manager, Service service, long instanceId, boolean isWritable,
            boolean isReadable, boolean hasEvents, String description, int maxValue, String unit) {
        super(manager, service, instanceId, "int", isWritable, isReadable, hasEvents, description);
        this.maxValue = maxValue;
    }

    @Override
    public boolean isHidden() {
        return false;
    }

    @Override
    public JsonObject toJson() {
        JsonObject base = super.toJson();
        base = enrich(base, "minValue", 0);
        base = enrich(base, "maxValue", maxValue);
        return enrich(base, "minStep", 1);
    }

    @Override
    protected Integer convert(JsonObject jsonObject) {
        if (jsonObject instanceof JsonNumber) {
            return ((JsonNumber) jsonObject).intValue();
        } else if (jsonObject == JsonValue.TRUE) {
            return 1; // For at least one enum type (locks), homekit will send a true instead of 1
        } else if (jsonObject == JsonValue.FALSE) {
            return 0;
        } else {
            throw new IndexOutOfBoundsException("Cannot convert " + jsonObject.getClass() + " to Integer");
        }
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
        return 0;
    }

}
