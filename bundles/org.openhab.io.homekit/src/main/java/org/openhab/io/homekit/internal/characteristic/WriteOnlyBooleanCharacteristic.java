/**
 *
 */
package org.openhab.io.homekit.internal.characteristic;

import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.hap.Service;

/**
 * @author Karel Goderis - Initial Contribution
 *
 */
@NonNullByDefault
public abstract class WriteOnlyBooleanCharacteristic extends GenericCharacteristic<Boolean> {

    public WriteOnlyBooleanCharacteristic(Service service, long instanceId, String description, String type) {
        super(service, instanceId, "bool", true, false, false, description, type);
        initializeValue();
    }

    public WriteOnlyBooleanCharacteristic(Service service, JsonValue value) {
        super(service, value);
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
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public Boolean toValue(JsonValue jsonValue) {
        if (jsonValue.getValueType().equals(ValueType.NUMBER)) {
            return ((JsonNumber) jsonValue).intValue() > 0;
        }
        return jsonValue.equals(JsonValue.TRUE);
    }

    @Override
    public Boolean toValue(State state) {
        OnOffType convertedState = state.as(OnOffType.class);
        if (convertedState == null) {
            return null;
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
    public JsonValue toValueJson(Boolean value) {
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
