package org.openhab.io.homekit.library.characteristic;

import java.math.BigDecimal;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.hap.HomekitService;
import org.openhab.io.homekit.internal.characteristic.HomekitFloatCharacteristic;
import org.openhab.io.homekit.internal.events.HomekitEventManager;

@NonNullByDefault
public class HomekitSaturationCharacteristic extends HomekitFloatCharacteristic {

    private static final String TYPE = "0000002F-0000-1000-8000-0026BB765291";

    public HomekitSaturationCharacteristic(HomekitService service, long instanceId, HomekitEventManager eventManager) {
        super(service, instanceId, true, true, true, "Adjust saturation of the light", 0, 100, 1, "percentage", TYPE, eventManager);
    }

    public HomekitSaturationCharacteristic(HomekitService service, JsonValue value, HomekitEventManager eventManager) {
        super(service, value, eventManager);
    }

    public static String getType() {
        return TYPE;
    }

    @Override
    public Double toValue(State state) {
        if (state instanceof HSBType) {
            PercentType saturation = ((HSBType) state).getSaturation();
            return saturation.doubleValue();
        } else {
            DecimalType convertedState = state.as(DecimalType.class);
            if (convertedState == null) {
                return minValue;
            }
            return convertedState.doubleValue();
        }
    }

    @Override
    public State toState(Double value) {
        return new HSBType(new PercentType(0), new PercentType(new BigDecimal(value)), new PercentType(100));
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
    public JsonValue toValueJson(@Nullable Double value) {
        return super.toValueJson(value);
    }

    @Override
    public JsonObject toJson(boolean includeMeta, boolean includePermissions, boolean includeType,
            boolean includeEvent) {
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

    public static String getTag() {
        return HomekitSaturationCharacteristic.class.getSimpleName().replace("HomekitCharacteristic", "");
    }
}
