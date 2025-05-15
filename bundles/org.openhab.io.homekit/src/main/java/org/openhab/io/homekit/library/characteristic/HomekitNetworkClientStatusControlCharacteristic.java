package org.openhab.io.homekit.library.characteristic;

import java.util.Map;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitTLV8Characteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Network Client Status Control Characteristic.
 * This characteristic represents the control configuration for network client status, as defined in the HomeKit
 * Accessory Protocol (HAP) specification.
 *
 * @author Karel Goderis - Initial Contribution
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "0000020D-0000-1000-8000-0026BB765291", name = "Network Client Status Control", tag = "networkClientStatusControl", acceptedItemTypes = {"String"})
@NonNullByDefault
public class HomekitNetworkClientStatusControlCharacteristic extends HomekitTLV8Characteristic {
    /**
     * Constructs a new Network Client Status Control characteristic.
     *
     * @param service the HomeKit service this characteristic belongs to
     * @param eventManager the event manager for handling HomeKit events
     * @param instanceId the instance ID for this characteristic
     */
    public HomekitNetworkClientStatusControlCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(true).withEvents(true).withTimedWrite(true)
                .withWriteResponse(true).withDescription("Network Client Status Control");
    }

    /**
     * Constructs a new Network Client Status Control characteristic from a JSON value.
     *
     * @param service the HomeKit service this characteristic belongs to
     * @param eventManager the event manager for handling HomeKit events
     * @param value the JSON value to initialize the characteristic with
     */
    public HomekitNetworkClientStatusControlCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    /**
     * Throws UnsupportedOperationException for TLV8 encoding (must be implemented for the specific device).
     */
    @Override
    protected byte[] encodeTLV8(Map<Integer, Object> value) {
        throw new UnsupportedOperationException("TLV8 encoding must be implemented for the specific device.");
    }

    /**
     * Throws UnsupportedOperationException for TLV8 decoding (must be implemented for the specific device).
     */
    @Override
    protected Map<Integer, Object> decodeTLV8(byte[] data) {
        throw new UnsupportedOperationException("TLV8 decoding must be implemented for the specific device.");
    }

    /**
     * Throws UnsupportedOperationException for default value (must be implemented for the specific device).
     */
    @Override
    public Map<Integer, Object> getDefault() {
        throw new UnsupportedOperationException("Default value must be implemented for the specific device.");
    }

    /**
     * Throws UnsupportedOperationException for JSON to TLV8 conversion (must be implemented for the specific device).
     */
    @Override
    public Map<Integer, Object> toValue(JsonValue jsonValue) {
        throw new UnsupportedOperationException("JSON to TLV8 conversion must be implemented for the specific device.");
    }

    /**
     * Throws UnsupportedOperationException for State to TLV8 conversion (must be implemented for the specific device).
     */
    @Override
    public Map<Integer, Object> toValue(State state) {
        throw new UnsupportedOperationException(
                "State to TLV8 conversion must be implemented for the specific device.");
    }

    /**
     * Throws UnsupportedOperationException for TLV8 to State conversion (must be implemented for the specific device).
     */
    @Override
    public State toState(Map<Integer, Object> value) {
        throw new UnsupportedOperationException(
                "TLV8 to State conversion must be implemented for the specific device.");
    }
}
