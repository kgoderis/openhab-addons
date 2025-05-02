package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.internal.characteristic.ReadOnlyStringCharacteristic;
import org.openhab.io.homekit.internal.events.HomekitEventManager;

@NonNullByDefault
public class ModelCharacteristic extends ReadOnlyStringCharacteristic {
    private static final String TYPE = "00000021-0000-1000-8000-0026BB765291";

    public ModelCharacteristic(Service service, long instanceId, HomekitEventManager eventManager) {
        super(service, instanceId, "Model", TYPE, eventManager);
    }

    public ModelCharacteristic(Service service, JsonValue value, HomekitEventManager eventManager) {
        super(service, value, eventManager);
    }

    public static String getType() {
        return TYPE;
    }

    @Override
    public State toState(String value) {
        return super.toState(value);
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
        return ModelCharacteristic.class.getSimpleName().replace("Characteristic", "");
    }
}
