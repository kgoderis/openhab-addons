package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.openhab.core.OpenHAB;
import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.internal.characteristic.ShortReadOnlyStringCharacteristic;

public class VersionCharacteristic extends ShortReadOnlyStringCharacteristic {
    private static final String TYPE = "00000037-0000-1000-8000-0026BB765291";

    public VersionCharacteristic(Service service, long instanceId) {
        super(service, instanceId, "1.0.0");
    }

    public VersionCharacteristic(Service service, JsonValue value) {
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
    public String getValue() {
        return OpenHAB.getVersion();
    }

    public void setVersion(String version) {
        setReadOnlyValue(version);
    }

    public static String getTag() {
        return VersionCharacteristic.class.getSimpleName().replace("Characteristic", "");
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
    public State toState(String value) {
        return super.toState(value);
    }

    @Override
    public JsonValue toValueJson(String value) {
        return super.toValueJson(value);
    }
}
