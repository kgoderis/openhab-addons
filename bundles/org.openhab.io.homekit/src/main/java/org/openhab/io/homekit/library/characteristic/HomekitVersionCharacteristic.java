package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.OpenHAB;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.hap.HomekitService;
import org.openhab.io.homekit.internal.characteristic.HomekitShortReadOnlyStringCharacteristic;
import org.openhab.io.homekit.internal.events.HomekitEventManager;

@NonNullByDefault
public class HomekitVersionCharacteristic extends HomekitShortReadOnlyStringCharacteristic {
    private static final String TYPE = "00000037-0000-1000-8000-0026BB765291";

    public HomekitVersionCharacteristic(HomekitService service, long instanceId, HomekitEventManager eventManager) {
        super(service, instanceId, "1.0.0", TYPE, eventManager);
    }

    public HomekitVersionCharacteristic(HomekitService service, JsonValue value, HomekitEventManager eventManager) {
        super(service, value, eventManager);
    }

    public static String getType() {
        return TYPE;
    }

    @Override
    public String getValue() {
        return OpenHAB.getVersion();
    }

    public void setVersion(String version) {
        setReadOnlyValue(version);
    }

    public static String getTag() {
        return HomekitVersionCharacteristic.class.getSimpleName().replace("HomekitCharacteristic", "");
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
    public State toState(String value) {
        return super.toState(value);
    }

    @Override
    public JsonValue toValueJson(@Nullable String value) {
        return super.toValueJson(value);
    }
}
