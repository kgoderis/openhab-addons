/**
 *
 */
package org.openhab.io.homekit.internal.characteristic;

import javax.json.JsonNumber;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

import org.openhab.core.library.types.OnOffType;
import org.openhab.core.types.State;
import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.ManagedService;

/**
 * @author Karel Goderis - Initial Contribution
 *
 */
public abstract class WriteOnlyBooleanCharacteristic extends AbstractManagedCharacteristic<Boolean> {

    public WriteOnlyBooleanCharacteristic(HomekitCommunicationManager manager, ManagedService service, long instanceId,
            String description) {
        super(manager, service, instanceId, "bool", true, false, false, description);
    }

    @Override
    public boolean isHidden() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    protected Boolean getDefault() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public Boolean getValue() throws Exception {
        throw new Exception("Can not read a write-only characteristic");
    }

    /** {@inheritDoc} */
    @Override
    public Boolean convert(JsonValue jsonValue) {
        if (jsonValue.getValueType().equals(ValueType.NUMBER)) {
            return ((JsonNumber) jsonValue).intValue() > 0;
        }
        return jsonValue.equals(JsonValue.TRUE);
    }

    @Override
    protected Boolean convert(State state) {
        OnOffType convertedState = state.as(OnOffType.class);
        if (convertedState == null) {
            return null;
        }

        return convertedState.equals(OnOffType.ON);
    }

    @Override
    protected State convert(Boolean value) {
        return value ? OnOffType.ON : OnOffType.OFF;
    }
}
