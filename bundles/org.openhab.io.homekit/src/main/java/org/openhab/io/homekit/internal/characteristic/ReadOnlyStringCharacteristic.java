/**
 *
 */
package org.openhab.io.homekit.internal.characteristic;

import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.openhab.core.library.types.StringType;
import org.openhab.core.types.State;
import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.Service;

/**
 * @author Karel Goderis - Initial Contribution
 *
 */
public abstract class ReadOnlyStringCharacteristic extends AbstractCharacteristic<String> {

    private static final int MAX_LEN = 255;

    private String value;

    public ReadOnlyStringCharacteristic(HomekitCommunicationManager manager, Service service, long instanceId,
            String description, String value) {
        super(manager, service, instanceId, "string", false, true, false, description);
        this.value = value;
    }

    @Override
    public boolean isHidden() {
        return false;
    }

    public void setReadOnlyValue(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value != null ? value : "Unavailable";
    }

    /** {@inheritDoc} */
    @Override
    protected String getDefault() {
        return "Unknown";
    }

    /** {@inheritDoc} */
    @Override
    public String convert(JsonValue jsonValue) {
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
        JsonObject base = toJson(true, true, includeType, includeType, includePermissions, includeMeta, includeEvent,
                false, true);
        return enrich(base, "maxLen", MAX_LEN);
    }

    @Override
    protected String convert(State state) {
        StringType convertedState = state.as(StringType.class);
        if (convertedState == null) {
            return null;
        }

        return convertedState.toFullString();
    }

    @Override
    protected State convert(String value) {
        return StringType.valueOf(value);
    }

}
