/**
 *
 */
package org.openhab.io.homekit.internal.characteristic;

import javax.json.JsonNumber;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.Service;
/**
 * @author Karel Goderis - Initial Contribution
 *
 */
public abstract class BooleanCharacteristic extends GenericCharacteristic<Boolean> {

    public BooleanCharacteristic(Service service, long instanceId,
            boolean isWritable, boolean isReadable, boolean hasEvents, String description) {
        super( service, instanceId, "bool", isWritable, isReadable, hasEvents, description);
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
    public Boolean toValue(JsonValue value) {
        if (value.getValueType().equals(ValueType.NUMBER)) {
            return ((JsonNumber) value).intValue() > 0;
        }
        return value.equals(JsonValue.TRUE);
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

    public static String getAcceptedItemType() {
        return CoreItemFactory.SWITCH;
    }
}
