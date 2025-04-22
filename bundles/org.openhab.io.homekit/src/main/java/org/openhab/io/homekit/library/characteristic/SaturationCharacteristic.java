package org.openhab.io.homekit.library.characteristic;

import java.math.BigDecimal;

import javax.json.JsonValue;

import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.internal.characteristic.FloatCharacteristic;

public class SaturationCharacteristic extends FloatCharacteristic {

    private static final String TYPE = "0000002F-0000-1000-8000-0026BB765291";

    public SaturationCharacteristic(Service service, long instanceId) {
        super(service, instanceId, true, true, true, "Adjust saturation of the light", 0, 100, 1, "percentage");
    }

    public SaturationCharacteristic(Service service, JsonValue value) {
        super(service, value);
    }

    public static String getType() {
        return TYPE;
    }

    @Override
    public String getInstanceType() {
        return getType();
    }

    @Override
    public Double toValue(State state) {
        if (state instanceof HSBType) {
            PercentType saturation = ((HSBType) state).getSaturation();
            return saturation.doubleValue();
        } else {
            DecimalType convertedState = state.as(DecimalType.class);
            if (convertedState == null) {
                return null;
            }
            return convertedState.doubleValue();
        }
    }

    @Override
    public State toState(Double value) {
        return new HSBType(new PercentType(0), new PercentType(new BigDecimal(value)), new PercentType(100));
        // State state = manager.getState(getChannelUID());

        // if (state instanceof HSBType) {
        // return new HSBType(((HSBType) state).getHue(), new PercentType(new BigDecimal(value)),
        // ((HSBType) state).getBrightness());
        // } else {
        // return new DecimalType(value);
    }

    public static String getTag() {
        return SaturationCharacteristic.class.getSimpleName().replace("Characteristic", "");
    }
    
    @Override
    public JsonObject toEventJson() {
        return super.toEventJson();
    }

    @Override
    public JsonObject toEventJson(Double value) {
        return super.toEventJson(value);
    }

    @Override
    public JsonObject toJson(boolean includeMeta, boolean includePermissions, boolean includeType, boolean includeEvent) {
        return super.toJson(includeMeta, includePermissions, includeType, includeEvent);
    }

    @Override
    public JsonObject toJson() {
        return super.toJson();
    }

    @Override
    public JsonObject toReducedJson() {
        return super.toReducedJson();
    }

    @Override
    public JsonValue toValueJson(Double value) {
        return super.toValueJson(value);
    }

}
