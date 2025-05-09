package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitFloatCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

@NonNullByDefault
public class HomekitTargetTemperatureCharacteristic extends HomekitFloatCharacteristic {

    private static final String TYPE = "00000035-0000-1000-8000-0026BB765291";

    public HomekitTargetTemperatureCharacteristic(HomekitService service, long instanceId, HomekitEventManager eventManager) {
        super(service, instanceId, true, true, true, "Target Temperature", 10, 38, 0.1, "°C", TYPE, eventManager);
    }

    public HomekitTargetTemperatureCharacteristic(HomekitService service, JsonValue value, HomekitEventManager eventManager) {
        super(service, value, eventManager);
    }

    public static String getType() {
        return TYPE;
    }

    public static String getTag() {
        return HomekitTargetTemperatureCharacteristic.class.getSimpleName().replace("HomekitCharacteristic", "");
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
    public JsonValue toValueJson(@Nullable Double value) {
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

    @Override
    public State toState(Double value) {
        return super.toState(value);
    }
}
