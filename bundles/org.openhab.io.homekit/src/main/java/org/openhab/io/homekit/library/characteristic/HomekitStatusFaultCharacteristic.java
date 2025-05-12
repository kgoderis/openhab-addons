package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Status Fault Characteristic.
 * This characteristic indicates if the accessory has a fault.
 *
 * @see <a href="https://developer.apple.com/documentation/homekit/hmcharacteristicstatusfault">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "00000077-0000-1000-8000-0026BB765291", name = "Status Fault", tag = "statusFault")
@NonNullByDefault
public class HomekitStatusFaultCharacteristic extends HomekitEnumCharacteristic {

    public enum StatusFault {
        NO_FAULT(0),
        GENERAL_FAULT(1);
        private final int code;
        StatusFault(int code) { this.code = code; }
        public int getCode() { return code; }
        public static StatusFault fromCode(int code) {
            for (StatusFault s : values()) {
                if (s.code == code) return s;
            }
            return NO_FAULT;
        }
    }

    public HomekitStatusFaultCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, StatusFault.values().length);
        withInstanceId(instanceId)
            .withPairedWrite(false)
            .withPairedRead(true)
            .withEvents(true)
            .withDescription("Status Fault");
    }

    public HomekitStatusFaultCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
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

    @Override
    public State toState(Integer value) {
        return super.toState(value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= 0 && value < StatusFault.values().length;
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(
            StatusFault.NO_FAULT.getCode(),
            StatusFault.GENERAL_FAULT.getCode()
        );
    }

    public void setValue(StatusFault value) throws Exception {
        setValue(value.getCode());
    }
}
