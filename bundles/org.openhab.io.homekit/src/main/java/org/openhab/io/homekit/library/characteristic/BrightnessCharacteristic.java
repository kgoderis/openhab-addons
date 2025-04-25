package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.internal.characteristic.IntegerCharacteristic;

@NonNullByDefault
public class BrightnessCharacteristic extends IntegerCharacteristic {

    private static final String TYPE = "00000008-0000-1000-8000-0026BB765291";

    public BrightnessCharacteristic(Service service, long instanceId) {
        super(service, instanceId, true, true, true, "Adjust brightness of a light", 0, 100, "percentage", TYPE);
    }

    public BrightnessCharacteristic(Service service, JsonValue value) {
        super(service, value);
    }

    public static String getType() {
        return TYPE;
    }

    @Override
    public Integer toValue(State state) {
        if (state instanceof HSBType) {
            PercentType brightness = ((HSBType) state).getBrightness();
            return brightness.intValue();
        } else {
            DecimalType convertedState = state.as(DecimalType.class);
            if (convertedState == null) {
                return minValue;
            }
            return convertedState.intValue();
        }
    }

    @Override
    public State toState(Integer value) {
        return new PercentType(value);

        // State state = manager.getState(getChannelUID());

        // if (state instanceof HSBType) {
        // return new HSBType(((HSBType) state).getHue(), ((HSBType) state).getSaturation(), new PercentType(value));
        // } else {
        // return new DecimalType(value);
        // }
    }

    public static String getTag() {
        return BrightnessCharacteristic.class.getSimpleName().replace("Characteristic", "");
    }

    @Override
    public JsonObject toEventJson(Integer value) {
        return super.toEventJson(value);
    }

    @Override
    public JsonObject toEventJson() {
        return super.toEventJson();
    }

    @Override
    public JsonValue toValueJson(Integer value) {
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
