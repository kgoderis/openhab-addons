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
 * HomeKit Crypto Hash Characteristic.
 * This characteristic represents a cryptographic hash value for secure operations.
 *
 * @author Karel Goderis - Initial Contribution
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "00000250-0000-1000-8000-0026BB765291", name = "Crypto Hash", tag = "cryptoHash", acceptedItemTypes = {"String"})
@NonNullByDefault
public class HomekitCryptoHashCharacteristic extends HomekitTLV8Characteristic {

    /**
     * Constructs a new Crypto Hash characteristic.
     *
     * @param service the HomeKit service this characteristic belongs to
     * @param eventManager the event manager for handling HomeKit events
     * @param instanceId the instance ID for this characteristic
     */
    public HomekitCryptoHashCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId).withPairedRead(false).withPairedWrite(true).withEvents(false)
                .withDescription("Crypto Hash");
    }

    /**
     * Constructs a new Crypto Hash characteristic from a JSON value.
     *
     * @param service the HomeKit service this characteristic belongs to
     * @param eventManager the event manager for handling HomeKit events
     * @param value the JSON value to initialize the characteristic with
     */
    public HomekitCryptoHashCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    /**
     * Returns the default value for this characteristic (empty map).
     */
    @Override
    public Map<Integer, Object> getDefault() {
        return java.util.Collections.emptyMap();
    }

    /**
     * Encodes the TLV8 map to a byte array. Not implemented for Crypto Hash.
     */
    @Override
    protected byte[] encodeTLV8(Map<Integer, Object> value) {
        throw new UnsupportedOperationException("TLV8 encoding not implemented for Crypto Hash");
    }

    /**
     * Decodes a byte array into a TLV8 map. Not implemented for Crypto Hash.
     */
    @Override
    protected Map<Integer, Object> decodeTLV8(byte[] data) {
        throw new UnsupportedOperationException("TLV8 decoding not implemented for Crypto Hash");
    }

    /**
     * Converts a State to a TLV8 map. Not implemented for Crypto Hash.
     */
    @Override
    public Map<Integer, Object> toValue(State state) {
        throw new UnsupportedOperationException("State conversion not implemented for Crypto Hash");
    }

    /**
     * Converts a TLV8 map to a State. Not implemented for Crypto Hash.
     */
    @Override
    public State toState(Map<Integer, Object> value) {
        throw new UnsupportedOperationException("State conversion not implemented for Crypto Hash");
    }
}
