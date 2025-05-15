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
 * HomeKit Supported RTP Configuration Characteristic.
 * <p>
 * This characteristic represents the supported RTP configuration for a device, using TLV8 encoding as defined by the
 * HAP specification.
 * <p>
 * See the HomeKit Accessory Protocol (HAP) specification for details: https://developer.apple.com/documentation/HomeKit
 *
 * @author Karel Goderis
 */
@HomekitCharacteristicType(type = "00000116-0000-1000-8000-0026BB765291", name = "Supported RTP Configuration", tag = "supportedRTPConfiguration", acceptedItemTypes = {"String"})
@NonNullByDefault
public class HomekitSupportedRTPConfigurationCharacteristic extends HomekitTLV8Characteristic {
    public HomekitSupportedRTPConfigurationCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId).withPairedRead(true).withDescription("Supported RTP Configuration");
    }

    public HomekitSupportedRTPConfigurationCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    /**
     * Encodes the TLV8 value for this characteristic.
     * 
     * @param value the value to encode
     * @return the encoded byte array
     */
    @Override
    protected byte[] encodeTLV8(Map<Integer, Object> value) {
        throw new UnsupportedOperationException("TLV8 encoding must be implemented for the specific device.");
    }

    /**
     * Decodes the TLV8 value for this characteristic.
     * 
     * @param data the byte array to decode
     * @return the decoded value as a map
     */
    @Override
    protected Map<Integer, Object> decodeTLV8(byte[] data) {
        throw new UnsupportedOperationException("TLV8 decoding must be implemented for the specific device.");
    }

    /**
     * Returns the default value for this characteristic.
     * 
     * @return the default value
     */
    @Override
    public Map<Integer, Object> getDefault() {
        throw new UnsupportedOperationException("Default value must be implemented for the specific device.");
    }

    /**
     * Converts a JSON value to the TLV8 value for this characteristic.
     * 
     * @param jsonValue the JSON value
     * @return the TLV8 value as a map
     */
    @Override
    public Map<Integer, Object> toValue(JsonValue jsonValue) {
        throw new UnsupportedOperationException("JSON to TLV8 conversion must be implemented for the specific device.");
    }

    /**
     * Converts a State to the TLV8 value for this characteristic.
     * 
     * @param state the State
     * @return the TLV8 value as a map
     */
    @Override
    public Map<Integer, Object> toValue(State state) {
        throw new UnsupportedOperationException(
                "State to TLV8 conversion must be implemented for the specific device.");
    }

    /**
     * Converts a TLV8 value to a State for this characteristic.
     * 
     * @param value the TLV8 value as a map
     * @return the State
     */
    @Override
    public State toState(Map<Integer, Object> value) {
        throw new UnsupportedOperationException(
                "TLV8 to State conversion must be implemented for the specific device.");
    }
}
