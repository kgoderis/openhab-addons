/**
 *
 */
package org.openhab.io.homekit.core.characteristic;

import java.util.Map;

import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.types.StringType;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * @author Karel Goderis - Initial Contribution
 *
 */
@NonNullByDefault
public abstract class HomekitStringCharacteristic extends AbstractHomekitCharacteristic<String> {

    private static final int MAX_LEN = 64;

    public HomekitStringCharacteristic(HomekitService service, HomekitEventManager eventManager) {
        super(service, eventManager);
        withFormat("string").withPairedWrite(true).withPairedRead(true).withEvents(true);
        initializeValue();
    }

    public HomekitStringCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
        initializeValue();
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
    public String toValue(JsonValue jsonValue, Map<String, Object> conversionMap) {
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
    public String toValue(State state, Map<String, Object> conversionMap) {
        StringType convertedState = state.as(StringType.class);
        if (convertedState == null) {
            return getDefault();
        }
        return convertedState.toFullString();
    }

    @Override
    public State toState(String value) {
        return new StringType(value);
    }

    public static String getAcceptedItemType() {
        return CoreItemFactory.STRING;
    }
}
