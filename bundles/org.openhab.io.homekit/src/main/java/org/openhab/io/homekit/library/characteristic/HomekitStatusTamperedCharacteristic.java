package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Status Tampered Characteristic.
 * This characteristic indicates if the accessory has been tampered with.
 *
 * @see <a href="https://developer.apple.com/documentation/homekit/hmcharacteristicstatustampered">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "0000007A-0000-1000-8000-0026BB765291", name = "Status Tampered", tag = "statusTampered")
@NonNullByDefault
public class HomekitStatusTamperedCharacteristic extends HomekitEnumCharacteristic {

    public enum StatusTampered {
        NOT_TAMPERED(0),
        TAMPERED(1);
        private final int code;
        StatusTampered(int code) { this.code = code; }
        public int getCode() { return code; }
        public static StatusTampered fromCode(int code) {
            for (StatusTampered v : values()) {
                if (v.code == code) return v;
            }
            return NOT_TAMPERED;
        }
    }

    public HomekitStatusTamperedCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, StatusTampered.values().length);
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(true)
            .withDescription("Status Tampered");
    }

    public HomekitStatusTamperedCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
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
    public State toState(Integer value) {
        return super.toState(value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && (value == StatusTampered.NOT_TAMPERED.getCode() || value == StatusTampered.TAMPERED.getCode());
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(StatusTampered.NOT_TAMPERED.getCode(), StatusTampered.TAMPERED.getCode());
    }

    public void setValue(StatusTampered value) throws Exception {
        setValue(value.getCode());
    }
}
