package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.OpenClosedType;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

@HomekitCharacteristicType(type = "0000006A-0000-1000-8000-0026BB765291", name = "Contact Sensor State", tag = "contactSensorState")
@NonNullByDefault
public class HomekitContactSensorStateCharacteristic extends HomekitEnumCharacteristic {

    public enum ContactSensorState {
        CONTACT_DETECTED(0),
        CONTACT_NOT_DETECTED(1);
        private final int code;
        ContactSensorState(int code) { this.code = code; }
        public int getCode() { return code; }
        public static ContactSensorState fromCode(int code) {
            for (ContactSensorState v : values()) {
                if (v.code == code) return v;
            }
            return CONTACT_DETECTED;
        }
    }

    public HomekitContactSensorStateCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, ContactSensorState.values().length);
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(true)
            .withDescription("Contact Sensor State");
    }

    public HomekitContactSensorStateCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
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

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && (value == ContactSensorState.CONTACT_DETECTED.getCode() || value == ContactSensorState.CONTACT_NOT_DETECTED.getCode());
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(ContactSensorState.CONTACT_DETECTED.getCode(), ContactSensorState.CONTACT_NOT_DETECTED.getCode());
    }

    public void setValue(ContactSensorState value) throws Exception {
        setValue(value.getCode());
    }
}
