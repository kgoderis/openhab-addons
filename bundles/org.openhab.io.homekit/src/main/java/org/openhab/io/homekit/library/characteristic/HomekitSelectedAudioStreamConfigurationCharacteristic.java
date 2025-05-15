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
 * HomeKit Selected Audio Stream Configuration Characteristic.
 * <p>
 * This characteristic represents the selected audio stream configuration for a device, as defined in the HomeKit
 * Accessory Protocol (HAP) specification.
 * <p>
 * See the HomeKit Accessory Protocol (HAP) specification for details: https://developer.apple.com/documentation/HomeKit
 *
 * @author Karel Goderis
 */
@HomekitCharacteristicType(type = "00000128-0000-1000-8000-0026BB765291", name = "Selected Audio Stream Configuration", tag = "selectedAudioStreamConfiguration", acceptedItemTypes = {"String"})
@NonNullByDefault
public class HomekitSelectedAudioStreamConfigurationCharacteristic extends HomekitTLV8Characteristic {
    /**
     * Constructs the characteristic for selected audio stream configuration.
     *
     * @param service the Homekit service
     * @param eventManager the event manager
     * @param instanceId the instance id
     */
    public HomekitSelectedAudioStreamConfigurationCharacteristic(HomekitService service,
            HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId).withPairedWrite(true).withPairedRead(true)
                .withDescription("Selected Audio Stream Configuration");
    }

    /**
     * Constructs the characteristic from a JSON value.
     *
     * @param service the Homekit service
     * @param eventManager the event manager
     * @param value the JSON value
     */
    public HomekitSelectedAudioStreamConfigurationCharacteristic(HomekitService service,
            HomekitEventManager eventManager, JsonValue value) {
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
     * @return the default value as a map
     */
    @Override
    public Map<Integer, Object> getDefault() {
        throw new UnsupportedOperationException("Default value must be implemented for the specific device.");
    }

    /**
     * Converts a JSON value to a TLV8 value.
     * 
     * @param jsonValue the JSON value
     * @return the TLV8 value as a map
     */
    @Override
    public Map<Integer, Object> toValue(JsonValue jsonValue) {
        throw new UnsupportedOperationException("JSON to TLV8 conversion must be implemented for the specific device.");
    }

    /**
     * Converts a State to a TLV8 value.
     * 
     * @param state the state
     * @return the TLV8 value as a map
     */
    @Override
    public Map<Integer, Object> toValue(State state) {
        throw new UnsupportedOperationException(
                "State to TLV8 conversion must be implemented for the specific device.");
    }

    /**
     * Converts a TLV8 value to a State.
     * 
     * @param value the TLV8 value as a map
     * @return the state
     */
    @Override
    public State toState(Map<Integer, Object> value) {
        throw new UnsupportedOperationException(
                "TLV8 to State conversion must be implemented for the specific device.");
    }
}
