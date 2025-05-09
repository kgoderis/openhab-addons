package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitReadOnlyStringCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

@NonNullByDefault
public class HomekitManufacturerCharacteristic extends HomekitReadOnlyStringCharacteristic {
    private static final String TYPE = "00000020-0000-1000-8000-0026BB765291";

    public HomekitManufacturerCharacteristic(HomekitService service, long instanceId, HomekitEventManager eventManager) {
        super(service, instanceId, "Manufacturer", TYPE, eventManager);
    }

    public HomekitManufacturerCharacteristic(HomekitService service, JsonValue value, HomekitEventManager eventManager) {
        super(service, value, eventManager);
    }

    public static String getType() {
        return TYPE;
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

    @Override
    public JsonObject toEventJson() {
        return super.toEventJson();
    }

    @Override
    public JsonObject toEventJson(String value) {
        return super.toEventJson(value);
    }

    @Override
    public JsonValue toValueJson(@Nullable String value) {
        return super.toValueJson(value);
    }

    public static String getTag() {
        return HomekitManufacturerCharacteristic.class.getSimpleName().replace("HomekitCharacteristic", "");
    }

    @Override
    public State toState(String value) {
        return super.toState(value);
    }
}
