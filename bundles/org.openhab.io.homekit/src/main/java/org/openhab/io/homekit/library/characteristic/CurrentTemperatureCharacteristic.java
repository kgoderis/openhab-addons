package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.internal.characteristic.FloatCharacteristic;

public class CurrentTemperatureCharacteristic extends FloatCharacteristic {

    public CurrentTemperatureCharacteristic(Service service, long instanceId) {
        super(service, instanceId, false, true, true, "Current temperature of the environment in Celsius", 0, 100, 0.1,
                "celcius");
    }

    public CurrentTemperatureCharacteristic(Service service, JsonValue value) {
        super(service, value);
    }

    public static String getType() {
        return "00000011-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }

    public static String getTag() {
        return CurrentTemperatureCharacteristic.class.getSimpleName().replace("Characteristic", "");
    }

    @Override
    public JsonObject toEventJson(Double value) {
        return super.toEventJson(value);
    }

    @Override
    public JsonObject toEventJson() {
        return super.toEventJson();
    }

    @Override
    public JsonValue toValueJson(Double value) {
        return super.toValueJson(value);
    }

    @Override
    public JsonObject toJson() {
        return super.toJson();
    }

    @Override
    public JsonObject toJson(boolean includeMeta, boolean includePermissions, boolean includeType, boolean includeEvent) {
        return super.toJson(includeMeta, includePermissions, includeType, includeEvent);
    }

    @Override
    public JsonObject toReducedJson() {
        return super.toReducedJson();
    }
}
