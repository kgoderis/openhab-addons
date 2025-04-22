package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.internal.characteristic.ReadOnlyStringCharacteristic;

public class FirmwareRevisionCharacteristic extends ReadOnlyStringCharacteristic {

    public FirmwareRevisionCharacteristic(Service service, long instanceId) {
        super(service, instanceId, "Firmware revision of the accessory");
    }

    public FirmwareRevisionCharacteristic(Service service, JsonValue value) {
        super(service, value);
    }

    public static String getType() {
        return "00000052-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }

    public static String getTag() {
        return FirmwareRevisionCharacteristic.class.getSimpleName().replace("Characteristic", "");
    }

    @Override
    public State toState(String value) {
        return super.toState(value);
    }

    @Override
    public JsonObject toEventJson() {
        return super.toEventJson();
    }

    @Override
    public JsonObject toEventJson(Double value) {
        return super.toEventJson(value);
    }

    @Override
    public JsonValue toValueJson(String value) {
        return super.toValueJson(value);
    }

    @Override
    public JsonObject toJson(boolean includeMeta, boolean includePermissions, boolean includeType, boolean includeEvent) {
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
}
