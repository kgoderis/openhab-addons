package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.internal.characteristic.ByteCharacteristic;

public class CurrentHeatingCoolingStateCharacteristic extends ByteCharacteristic {

    public CurrentHeatingCoolingStateCharacteristic(Service service, long instanceId) {
        super(service, instanceId, false, true, true, "Current heating cooling state", (byte) 0, (byte) 3);
    }

    public CurrentHeatingCoolingStateCharacteristic(Service service, JsonValue value) {
        super(service, value);
    }

    public static String getType() {
        return "0000000F-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }

    public static String getTag() {
        return CurrentHeatingCoolingStateCharacteristic.class.getSimpleName().replace("Characteristic", "");
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
    public JsonValue toValueJson(Byte value) {
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

    @Override
    public State toState(Byte value) {
        return super.toState(value);
    }
}
