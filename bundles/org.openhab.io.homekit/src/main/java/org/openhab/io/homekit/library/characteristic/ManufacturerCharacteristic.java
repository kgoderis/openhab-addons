package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.internal.characteristic.ReadOnlyStringCharacteristic;

public class ManufacturerCharacteristic extends ReadOnlyStringCharacteristic {
    private static final String TYPE = "00000020-0000-1000-8000-0026BB765291";

    public ManufacturerCharacteristic(Service service, long instanceId) {
        super(service, instanceId, "Manufacturer of the accessory");
    }

    public ManufacturerCharacteristic(Service service, JsonValue value) {
        super(service, value);
    }

    public static String getType() {
        return TYPE;
    }

    @Override
    public String getInstanceType() {
        return getType();
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

    @Override
    public JsonObject toEventJson() {
        return super.toEventJson();
    }

    @Override
    public JsonObject toEventJson(String value) {
        return super.toEventJson(value);
    }

    @Override
    public JsonValue toValueJson(String value) {
        return super.toValueJson(value);
    }

    public static String getTag() {
        return ManufacturerCharacteristic.class.getSimpleName().replace("Characteristic", "");
    }
    @Override
    public State toState(String value) {
        return super.toState(value);
    }
}
