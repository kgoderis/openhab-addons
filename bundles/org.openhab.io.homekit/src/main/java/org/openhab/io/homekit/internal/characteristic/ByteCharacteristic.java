package org.openhab.io.homekit.internal.characteristic;

import javax.json.JsonNumber;
import javax.json.JsonObject;

import org.openhab.core.library.types.DecimalType;
import org.openhab.core.types.State;
import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.Service;

public abstract class ByteCharacteristic extends AbstractCharacteristic<Byte> {

    private final byte minValue;
    private final byte maxValue;

    public ByteCharacteristic(HomekitCommunicationManager manager, Service service, long instanceId, boolean isWritable,
            boolean isReadable, boolean hasEvents, String description, byte minValue, byte maxValue) {
        super(manager, service, instanceId, "uint8", isWritable, isReadable, hasEvents, description);
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
    protected Byte convert(JsonObject jsonObject) {
        return (byte) ((JsonNumber) jsonObject).intValue();
    }

    @Override
    protected Byte convert(State state) {
        DecimalType convertedState = state.as(DecimalType.class);
        if (convertedState == null) {
            return null;
        }

        return (byte) convertedState.intValue();
    }

    @Override
    protected State convert(Byte value) {
        return new DecimalType(value);
    }

    @Override
    protected Byte getDefault() {
        return minValue;
    }

}
