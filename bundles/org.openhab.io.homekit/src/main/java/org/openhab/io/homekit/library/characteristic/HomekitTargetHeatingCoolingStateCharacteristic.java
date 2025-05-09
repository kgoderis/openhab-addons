package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitByteCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

@NonNullByDefault
public class HomekitTargetHeatingCoolingStateCharacteristic extends HomekitByteCharacteristic {

    private static final String TYPE = "00000033-0000-1000-8000-0026BB765291";

    public HomekitTargetHeatingCoolingStateCharacteristic(HomekitService service, long instanceId, HomekitEventManager eventManager) {
        super(service, instanceId, true, true, true, "Target Heating Cooling State", (byte) 0, (byte) 3, TYPE, eventManager);
    }

    public HomekitTargetHeatingCoolingStateCharacteristic(HomekitService service, JsonValue value, HomekitEventManager eventManager) {
        super(service, value, eventManager);
    }

    public static String getType() {
        return TYPE;
    }

    public static String getTag() {
        return HomekitTargetHeatingCoolingStateCharacteristic.class.getSimpleName().replace("HomekitCharacteristic", "");
    }

    @Override
    public State toState(Byte value) {
        return super.toState(value);
    }

    @Override
    public JsonObject toEventJson(Byte value) {
        return super.toEventJson(value);
    }

    @Override
    public JsonObject toEventJson() {
        return super.toEventJson();
    }

    @Override
    public JsonValue toValueJson(@Nullable Byte value) {
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
