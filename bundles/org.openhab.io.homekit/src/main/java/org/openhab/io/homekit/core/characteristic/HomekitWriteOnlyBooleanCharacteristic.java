/**
 *
 */
package org.openhab.io.homekit.core.characteristic;

import java.util.Map;

import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * @author Karel Goderis - Initial Contribution
 *
 */
@NonNullByDefault
public abstract class HomekitWriteOnlyBooleanCharacteristic extends AbstractHomekitCharacteristic<Boolean> {

    public HomekitWriteOnlyBooleanCharacteristic(HomekitService service, HomekitEventManager eventManager) {
        super(service, eventManager);
        withFormat("bool").withPairedWrite(true).withPairedRead(false).withEvents(false);
        initializeValue();
    }

    public HomekitWriteOnlyBooleanCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
        initializeValue();
    }

    @Override
    public boolean isHidden() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public Boolean getDefault() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public Boolean getValue() {
        return getDefault();
    }

    /** {@inheritDoc} */
    @Override
    public Boolean toValue(JsonValue jsonValue, Map<String, Object> conversionMap) {
        if (jsonValue.getValueType().equals(ValueType.NUMBER)) {
            return ((JsonNumber) jsonValue).intValue() > 0;
        }
        return jsonValue.equals(JsonValue.TRUE);
    }

    @Override
    public Boolean toValue(State state, Map<String, Object> conversionMap) {
        OnOffType convertedState = state.as(OnOffType.class);
        if (convertedState == null) {
            return getDefault();
        }

        return convertedState.equals(OnOffType.ON);
    }

    @Override
    public State toState(Boolean value) {
        return value ? OnOffType.ON : OnOffType.OFF;
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
    public JsonValue toValueJson(@Nullable Boolean value) {
        return super.toValueJson(value);
    }

    @Override
    public JsonObject toJson() {
        return super.toJson();
    }

    @Override
    public JsonObject toJson(boolean includeMeta, boolean includePermissions, boolean includeType,
            boolean includeEvent) {
        return super.toJson(includeMeta, includePermissions, includeType, includeEvent);
    }

    @Override
    public JsonObject toReducedJson() {
        return super.toReducedJson();
    }

    public static String getAcceptedItemType() {
        return CoreItemFactory.SWITCH;
    }
}
