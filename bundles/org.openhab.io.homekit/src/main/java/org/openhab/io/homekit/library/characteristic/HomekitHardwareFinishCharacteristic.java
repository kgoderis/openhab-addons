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
 * HomeKit Hardware Finish Characteristic.
 * <p>
 * This characteristic represents the hardware finish in TLV8 format. It provides information about the physical
 * appearance and finish of the hardware, such as brushed, polished, or matte. The value is encoded as TLV8 and must be
 * interpreted according to the device's capabilities and the HAP specification.
 * <p>
 * See the HomeKit Accessory Protocol (HAP) specification for details: https://developer.apple.com/documentation/HomeKit
 *
 * @author Karel Goderis
 */
@HomekitCharacteristicType(type = "0000026C-0000-1000-8000-0026BB765291", name = "Hardware Finish", tag = "hardwareFinish", acceptedItemTypes = {"String"})
@NonNullByDefault
public class HomekitHardwareFinishCharacteristic extends HomekitTLV8Characteristic {
    /**
     * Constructs a new Hardware Finish characteristic.
     *
     * @param service the HomeKit service this characteristic belongs to
     * @param eventManager the event manager for handling HomeKit events
     * @param instanceId the instance ID for this characteristic
     */
    public HomekitHardwareFinishCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(false).withEvents(true)
                .withDescription("Hardware Finish");
    }

    /**
     * Constructs a new Hardware Finish characteristic from a JSON value.
     *
     * @param service the HomeKit service this characteristic belongs to
     * @param eventManager the event manager for handling HomeKit events
     * @param value the JSON value to initialize the characteristic with
     */
    public HomekitHardwareFinishCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    /**
     * Encodes the TLV8 value for the hardware finish.
     *
     * @param value the value to encode
     * @return the encoded byte array
     * @throws UnsupportedOperationException always, must be implemented for the specific device
     */
    @Override
    protected byte[] encodeTLV8(Map<Integer, Object> value) {
        throw new UnsupportedOperationException("TLV8 encoding must be implemented for the specific device.");
    }

    /**
     * Decodes the TLV8 value for the hardware finish.
     *
     * @param data the byte array to decode
     * @return the decoded map
     * @throws UnsupportedOperationException always, must be implemented for the specific device
     */
    @Override
    protected Map<Integer, Object> decodeTLV8(byte[] data) {
        throw new UnsupportedOperationException("TLV8 decoding must be implemented for the specific device.");
    }

    /**
     * Gets the default value for the hardware finish.
     *
     * @return the default value map
     * @throws UnsupportedOperationException always, must be implemented for the specific device
     */
    @Override
    public Map<Integer, Object> getDefault() {
        throw new UnsupportedOperationException("Default value must be implemented for the specific device.");
    }

    /**
     * Converts a JSON value to a TLV8 value for the hardware finish.
     *
     * @param jsonValue the JSON value to convert
     * @return the TLV8 value map
     * @throws UnsupportedOperationException always, must be implemented for the specific device
     */
    @Override
    public Map<Integer, Object> toValue(JsonValue jsonValue) {
        throw new UnsupportedOperationException("JSON to TLV8 conversion must be implemented for the specific device.");
    }

    /**
     * Converts a State to a TLV8 value for the hardware finish.
     *
     * @param state the state to convert
     * @return the TLV8 value map
     * @throws UnsupportedOperationException always, must be implemented for the specific device
     */
    @Override
    public Map<Integer, Object> toValue(State state) {
        throw new UnsupportedOperationException(
                "State to TLV8 conversion must be implemented for the specific device.");
    }

    /**
     * Converts a TLV8 value to a State for the hardware finish.
     *
     * @param value the TLV8 value map
     * @return the corresponding State
     * @throws UnsupportedOperationException always, must be implemented for the specific device
     */
    @Override
    public State toState(Map<Integer, Object> value) {
        throw new UnsupportedOperationException(
                "TLV8 to State conversion must be implemented for the specific device.");
    }
}
