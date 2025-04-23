package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.internal.characteristic.FloatCharacteristic;

public class HueCharacteristic extends FloatCharacteristic {

    private static final String TYPE = "00000013-0000-1000-8000-0026BB765291";

    public HueCharacteristic(Service service, long instanceId) {
        super(service, instanceId, true, true, true, "Adjust hue of the light", 0, 360, 1, "arcdegrees");
    }

    public HueCharacteristic(Service service, JsonValue value) {
        super(service, value);
    }

    @Override
    public static String getType() {
        return TYPE;
    }

    @Override
    public String getInstanceType() {
        return TYPE;
    }

    @Override
    public Double toValue(State state) {
        if (state instanceof HSBType) {
            DecimalType hue = ((HSBType) state).getHue();
            return hue.doubleValue();
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
        return new DecimalType(value);
        // return new HSBType(value, 100, 100);

        // State state = manager.getState(getChannelUID());

        // if (state instanceof HSBType) {
        // return new HSBType(new DecimalType(value), ((HSBType) state).getSaturation(),
        // ((HSBType) state).getBrightness());
        // } else {
        // return new DecimalType(value);
        // }
    }

    public static String getTag() {
        return HueCharacteristic.class.getSimpleName().replace("Characteristic", "");
    }

    @Override
    public JsonObject toEventJson(Double value) {
        return super.toEventJson(value);
    }

    @Override
    public JsonObject toEventJson() {
        return super.toEventJson();
    }

    @Override
    public JsonValue toValueJson(Double value) {
        return super.toValueJson(value);
    }

    @Override
    public JsonObject toJson() {
        return super.toJson();
    }

    @Override
    public JsonObject toJson(boolean includeMeta, boolean includePermissions, boolean includeType,
            boolean includeEvent) {
        return super.toJson(includeMeta, includePermissions, includeType, includeEvent);
    }

    @Override
    public JsonObject toReducedJson() {
        return super.toReducedJson();
    }
}
