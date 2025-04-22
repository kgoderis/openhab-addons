/**
 *
 */
package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.internal.characteristic.BooleanCharacteristic;

/**
 * @author kgoderis
 *
 */
public class StatusActiveCharacteristic extends BooleanCharacteristic {

    private static final String TYPE = "00000075-0000-1000-8000-0026BB765291";

    public StatusActiveCharacteristic(Service service, long instanceId) {
        super(service, instanceId, false, true, true, "Status active");
    }

    public StatusActiveCharacteristic(Service service, JsonValue value) {
        super(service, value);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String getInstanceType() {
        return getType();
    }

    public static String getTag() {
        return StatusActiveCharacteristic.class.getSimpleName().replace("Characteristic", "");
    }

    @Override
    public JsonObject toEventJson(Boolean value) {
        return super.toEventJson(value);
    }

    @Override
    public JsonObject toEventJson() {
        return super.toEventJson();
    }

    @Override
    public JsonValue toValueJson(Boolean value) {
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
    public State toState(Boolean value) {
        return super.toState(value);
    }
}
