/**
 *
 */
package org.openhab.io.homekit.internal.characteristic;

import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.types.StringType;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.hap.Service;

/**
 * @author Karel Goderis - Initial Contribution
 *
 */
public abstract class StringCharacteristic extends GenericCharacteristic<String> {

    private static final int MAX_LEN = 64;

    public StringCharacteristic(Service service, long instanceId, String description) {
        super(service, instanceId, "string", true, true, true, description);
    }

    public StringCharacteristic(Service service, JsonValue value) {
        super(service, value);
    }

    @Override
    public boolean isHidden() {
        return false;
    }

    @Override
    public String getDefault() {
        return "Unknown";
    }

    @Override
    public String toValue(JsonValue jsonValue) {
        return ((JsonString) jsonValue).getString();
    }

    @Override
    public JsonObject toJson() {
        JsonObject base = super.toJson();
        return enrich(base, "maxLen", MAX_LEN);
    }

    @Override
    public JsonObject toReducedJson() {
        JsonObject base = super.toReducedJson();
        return enrich(base, "maxLen", MAX_LEN);
    }

    @Override
    public JsonObject toJson(boolean includeMeta, boolean includePermissions, boolean includeType,
            boolean includeEvent) {
        JsonObject base = super.toJson(includeMeta, includePermissions, includeType, includeEvent);
        return enrich(base, "maxLen", MAX_LEN);
    }

    @Override
    public String toValue(State state) {
        StringType convertedState = state.as(StringType.class);
        if (convertedState == null) {
            return null;
        }
        return convertedState.toFullString();
    }

    @Override
    public State toState(String value) {
        return new StringType(value);
    }

    @Override
    public JsonObject toEventJson(String value) {
        return super.toEventJson(value);
    }

    @Override
    public JsonObject toEventJson() {
        return super.toEventJson();
    }

    @Override
    public JsonValue toValueJson(String value) {
        return super.toValueJson(value);
    }

    public static String getAcceptedItemType() {
        return CoreItemFactory.STRING;
    }
}
