package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.OpenClosedType;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.internal.characteristic.EnumCharacteristic;
import org.openhab.io.homekit.internal.events.HomekitEventManager;

@NonNullByDefault
public class ContactSensorStateCharacteristic extends EnumCharacteristic {

    private static final String TYPE = "0000006A-0000-1000-8000-0026BB765291";

    public ContactSensorStateCharacteristic(Service service, long instanceId, HomekitEventManager eventManager) {
        super(service, instanceId, false, true, true, "Contact Sensor State", 1, TYPE, eventManager);
    }

    public ContactSensorStateCharacteristic(Service service, JsonValue value, HomekitEventManager eventManager) {
        super(service, value, eventManager);
    }

    public static String getType() {
        return TYPE;
    }

    @Override
    public Integer toValue(State state) {
        if (state instanceof OpenClosedType) {
            return ((OpenClosedType) state) == OpenClosedType.OPEN ? 0 : 1;
        }
        return super.toValue(state);
    }

    @Override
    public State toState(Integer value) {
        return value == 0 ? OpenClosedType.OPEN : OpenClosedType.CLOSED;
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
    public JsonValue toValueJson(@Nullable Integer value) {
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

    public static String getTag() {
        return ContactSensorStateCharacteristic.class.getSimpleName().replace("Characteristic", "");
    }
}
