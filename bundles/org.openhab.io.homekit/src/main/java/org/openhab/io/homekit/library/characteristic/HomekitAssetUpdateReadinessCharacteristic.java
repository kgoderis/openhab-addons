package org.openhab.io.homekit.library.characteristic;

import java.util.Map;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitTLV8Characteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Asset Update Readiness Characteristic.
 * This characteristic represents the readiness state for asset updates in TLV8 format.
 * It provides information about whether the accessory is ready to receive and process asset updates.
 *
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "00000269-0000-1000-8000-0026BB765291", name = "Asset Update Readiness", tag = "assetUpdateReadiness", acceptedItemTypes = {
        "String" })
@NonNullByDefault
public class HomekitAssetUpdateReadinessCharacteristic extends HomekitTLV8Characteristic {
    /**
     * Creates a new Asset Update Readiness characteristic.
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param instanceId The instance ID for this characteristic
     */
    public HomekitAssetUpdateReadinessCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(false).withEvents(true)
                .withDescription("Asset Update Readiness");
    }

    /**
     * Creates a new Asset Update Readiness characteristic from a JSON value.
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param value The JSON value to initialize the characteristic with
     */
    public HomekitAssetUpdateReadinessCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    /**
     * Encodes the TLV8 data for this characteristic.
     * Must be implemented by the specific device implementation.
     *
     * @param value The value to encode
     * @return The encoded TLV8 data
     * @throws UnsupportedOperationException if not implemented by the device
     */
    @Override
    protected byte[] encodeTLV8(Map<Integer, Object> value) {
        throw new UnsupportedOperationException("TLV8 encoding must be implemented for the specific device.");
    }

    /**
     * Decodes the TLV8 data for this characteristic.
     * Must be implemented by the specific device implementation.
     *
     * @param data The TLV8 data to decode
     * @return The decoded value
     * @throws UnsupportedOperationException if not implemented by the device
     */
    @Override
    protected Map<Integer, Object> decodeTLV8(byte[] data) {
        throw new UnsupportedOperationException("TLV8 decoding must be implemented for the specific device.");
    }

    /**
     * Gets the default value for this characteristic.
     * Must be implemented by the specific device implementation.
     *
     * @return The default value
     * @throws UnsupportedOperationException if not implemented by the device
     */
    @Override
    public Map<Integer, Object> getDefault() {
        throw new UnsupportedOperationException("Default value must be implemented for the specific device.");
    }

    /**
     * Converts a JSON value to TLV8 format.
     * Must be implemented by the specific device implementation.
     *
     * @param jsonValue The JSON value to convert
     * @return The converted TLV8 value
     * @throws UnsupportedOperationException if not implemented by the device
     */
    @Override
    public Map<Integer, Object> toValue(JsonValue jsonValue) {
        throw new UnsupportedOperationException("JSON to TLV8 conversion must be implemented for the specific device.");
    }

    /**
     * Converts an OpenHAB state to TLV8 format.
     * Must be implemented by the specific device implementation.
     *
     * @param state The OpenHAB state to convert
     * @return The converted TLV8 value
     * @throws UnsupportedOperationException if not implemented by the device
     */
    @Override
    public Map<Integer, Object> toValue(org.openhab.core.types.State state) {
        throw new UnsupportedOperationException(
                "State to TLV8 conversion must be implemented for the specific device.");
    }

    /**
     * Converts a TLV8 value to an OpenHAB state.
     * Must be implemented by the specific device implementation.
     *
     * @param value The TLV8 value to convert
     * @return The converted OpenHAB state
     * @throws UnsupportedOperationException if not implemented by the device
     */
    @Override
    public org.openhab.core.types.State toState(Map<Integer, Object> value) {
        throw new UnsupportedOperationException(
                "TLV8 to State conversion must be implemented for the specific device.");
    }
}
