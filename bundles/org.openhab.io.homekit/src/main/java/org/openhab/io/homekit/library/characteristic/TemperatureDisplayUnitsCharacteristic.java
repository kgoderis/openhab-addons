package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.openhab.core.types.State;
import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.internal.characteristic.ByteCharacteristic;

public class TemperatureDisplayUnitsCharacteristic extends ByteCharacteristic {

    public TemperatureDisplayUnitsCharacteristic(Service service, long instanceId) {
        super(service, instanceId, true, true, true, "Temperature display units", (byte) 0, (byte) 1);
    }

    public TemperatureDisplayUnitsCharacteristic(Service service, JsonValue value) {
        super(service, value);
    }

    public static String getType() {
        return "00000036-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }

    @Override
    public State toState(Byte value) {
        return super.toState(value);
    }

    public static String getTag() {
        return TemperatureDisplayUnitsCharacteristic.class.getSimpleName().replace("Characteristic", "");
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
}
