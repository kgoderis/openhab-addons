package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitTLV8Characteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import java.util.Map;
import org.openhab.core.types.State;

/**
 * HomeKit Access Code Supported Configuration Characteristic.
 * <p>
 * This characteristic represents the supported configuration for access codes, indicating which access code formats and options are supported by the device. The value is encoded as TLV8 and must be interpreted according to the HAP specification.
 * <p>
 * See the HomeKit Accessory Protocol (HAP) specification for details: https://developer.apple.com/documentation/HomeKit
 *
 * @author Karel Goderis
 */
@HomekitCharacteristicType(type = "00000261-0000-1000-8000-0026BB765291", name = "Access Code Supported Configuration", tag = "accessCodeSupportedConfiguration")
@NonNullByDefault
public class HomekitAccessCodeSupportedConfigurationCharacteristic extends HomekitTLV8Characteristic {
    /**
     * Constructs a new Access Code Supported Configuration characteristic.
     *
     * @param service the HomeKit service this characteristic belongs to
     * @param eventManager the event manager for handling HomeKit events
     * @param instanceId the instance ID for this characteristic
     */
    public HomekitAccessCodeSupportedConfigurationCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId)
            .withPairedWrite(false)
            .withPairedRead(true)
            .withEvents(false)
            .withDescription("Access Code Supported Configuration");
    }

    /**
     * Constructs a new Access Code Supported Configuration characteristic from a JSON value.
     *
     * @param service the HomeKit service this characteristic belongs to
     * @param eventManager the event manager for handling HomeKit events
     * @param value the JSON value to initialize the characteristic with
     */
    public HomekitAccessCodeSupportedConfigurationCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    /**
     * Encodes the TLV8 value for the access code supported configuration.
     *
     * @param value the value to encode
     * @return the encoded byte array
     * @throws UnsupportedOperationException always, must be implemented for the specific device
     */
    @Override
    protected byte[] encodeTLV8(Map<Integer, Object> value) {
        throw new UnsupportedOperationException("Encoding not implemented");
    }

    /**
     * Decodes the TLV8 value for the access code supported configuration.
     *
     * @param data the byte array to decode
     * @return the decoded map
     * @throws UnsupportedOperationException always, must be implemented for the specific device
     */
    @Override
    protected Map<Integer, Object> decodeTLV8(byte[] data) {
        throw new UnsupportedOperationException("Decoding not implemented");
    }

    /**
     * Gets the default value for the access code supported configuration.
     *
     * @return the default value map
     * @throws UnsupportedOperationException always, must be implemented for the specific device
     */
    @Override
    public Map<Integer, Object> getDefault() {
        throw new UnsupportedOperationException("Default value not implemented");
    }

    /**
     * Converts a State to a TLV8 value for the access code supported configuration.
     *
     * @param state the state to convert
     * @return the TLV8 value map
     * @throws UnsupportedOperationException always, must be implemented for the specific device
     */
    @Override
    public Map<Integer, Object> toValue(State state) {
        throw new UnsupportedOperationException("State to TLV8 not implemented");
    }

    /**
     * Converts a TLV8 value to a State for the access code supported configuration.
     *
     * @param value the TLV8 value map
     * @return the corresponding State
     * @throws UnsupportedOperationException always, must be implemented for the specific device
     */
    @Override
    public State toState(Map<Integer, Object> value) {
        throw new UnsupportedOperationException("TLV8 to State not implemented");
    }
} 