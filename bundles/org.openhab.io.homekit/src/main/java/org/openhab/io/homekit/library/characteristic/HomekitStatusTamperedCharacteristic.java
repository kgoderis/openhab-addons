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
 * The status can be one of: NOT_TAMPERED (0) or TAMPERED (1).
 * This is used to report if the accessory has been physically tampered with.
 *
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "0000007A-0000-1000-8000-0026BB765291", name = "Status Tampered", tag = "statusTampered", acceptedItemTypes = {
        "Number", "String" })
@NonNullByDefault
public class HomekitStatusTamperedCharacteristic extends HomekitEnumCharacteristic {

    public enum StatusTampered {
        NOT_TAMPERED(0),
        TAMPERED(1);

        private final int value;

        StatusTampered(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static StatusTampered fromValue(int value) {
            for (StatusTampered status : values()) {
                if (status.value == value) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Invalid Status Tampered value: " + value);
        }
    }

    public HomekitStatusTamperedCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, 2);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(false).withEvents(true)
                .withDescription("Status Tampered");
    }

    public HomekitStatusTamperedCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
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
        if (value == null)
            return false;
        try {
            StatusTampered.fromValue(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        java.util.Set<Integer> allowed = new java.util.HashSet<>();
        for (StatusTampered status : StatusTampered.values()) {
            allowed.add(status.getValue());
        }
        return allowed;
    }

    public void setValue(StatusTampered value) throws Exception {
        setValue(value.getValue());
    }
}
