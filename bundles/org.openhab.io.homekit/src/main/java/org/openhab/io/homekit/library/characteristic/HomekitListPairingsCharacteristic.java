package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitTLV8Characteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit List Pairings Characteristic.
 * This characteristic represents the TLV8 value for HomeKit list pairings.
 *
 * @see <a href=\"https://developer.apple.com/documentation/HomeKit\">HAP Specification</a>
 * @author Karel Goderis - Initial Contribution
 */
@HomekitCharacteristicType(type = "00000050-0000-1000-8000-0026BB765291", name = "List Pairings", tag = "listPairings", acceptedItemTypes = {
        "String" })
@NonNullByDefault
public class HomekitListPairingsCharacteristic extends HomekitTLV8Characteristic {

    public HomekitListPairingsCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId).withPairedWrite(true).withPairedRead(true).withEvents(true)
                .withDescription("List Pairings");
    }

    public HomekitListPairingsCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    /**
     * Encodes the TLV8 value for list pairings.
     *
     * @param value the value to encode
     * @return the encoded byte array
     */
    @Override
    protected byte[] encodeTLV8(java.util.Map<Integer, Object> value) {
        return new byte[0];
    }

    /**
     * Decodes the TLV8 value for list pairings.
     *
     * @param data the byte array to decode
     * @return the decoded map
     */
    @Override
    protected java.util.Map<Integer, Object> decodeTLV8(byte[] data) {
        return java.util.Collections.emptyMap();
    }

    /**
     * Gets the default value for list pairings.
     *
     * @return the default value map
     */
    @Override
    public java.util.Map<Integer, Object> getDefault() {
        return java.util.Collections.emptyMap();
    }

    /**
     * Converts a State to a TLV8 value for list pairings.
     *
     * @param state the state to convert
     * @return the TLV8 value map
     */
    @Override
    public java.util.Map<Integer, Object> toValue(org.openhab.core.types.State state) {
        return java.util.Collections.emptyMap();
    }

    /**
     * Converts a TLV8 value to a State for list pairings.
     *
     * @param value the TLV8 value map
     * @return the corresponding State
     */
    @Override
    public State toState(java.util.Map<Integer, Object> value) {
        throw new UnsupportedOperationException(
                "TLV8 to State conversion must be implemented for the specific device.");
    }
}
