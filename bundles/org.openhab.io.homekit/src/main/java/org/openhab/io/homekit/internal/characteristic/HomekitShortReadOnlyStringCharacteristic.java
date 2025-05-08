/**
 *
 */
package org.openhab.io.homekit.internal.characteristic;

import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.types.StringType;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.hap.HomekitService;
import org.openhab.io.homekit.internal.events.HomekitEventManager;

/**
 * @author Karel Goderis - Initial Contribution
 *
 */
@NonNullByDefault
public abstract class HomekitShortReadOnlyStringCharacteristic extends HomekitBaseCharacteristic<String> {

    private static final int MAX_LEN = 64;

    public HomekitShortReadOnlyStringCharacteristic(HomekitService service, long instanceId, String description, String type, HomekitEventManager eventManager) {
        super(service, instanceId, "string", false, true, false, description, type, eventManager);
        initializeValue();
    }

    public HomekitShortReadOnlyStringCharacteristic(HomekitService service, JsonValue value, HomekitEventManager eventManager) {
        super(service, value, eventManager);
        initializeValue();
    }

    public void setReadOnlyValue(String value) {
        this.value = value;
    }

    /** {@inheritDoc} */
    @Override
    public void setValue(@Nullable String value) throws Exception {
        throw new Exception("Can not modify a readonly characteristic");
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
            return getDefault();
        }
        return convertedState.toFullString();
    }

    @Override
    public State toState(String value) {
        return StringType.valueOf(value);
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
    public JsonValue toValueJson(@Nullable String value) {
        return super.toValueJson(value);
    }

    public static String getAcceptedItemType() {
        return CoreItemFactory.STRING;
    }
}
