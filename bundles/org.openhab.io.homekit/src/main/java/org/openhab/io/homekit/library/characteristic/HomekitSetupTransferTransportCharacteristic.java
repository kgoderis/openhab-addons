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
 * HomeKit Setup Transfer Transport Characteristic.
 * This characteristic represents the setup configuration for transfer transport in TLV8 format.
 * It provides information about how data should be transferred between devices.
 * The TLV8 format allows for flexible encoding of transport-specific configuration data.
 *
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "00000201-0000-1000-8000-0026BB765291", name = "Setup Transfer Transport", tag = "setupTransferTransport", acceptedItemTypes = {"String"})
@NonNullByDefault
public class HomekitSetupTransferTransportCharacteristic extends HomekitTLV8Characteristic {
    public HomekitSetupTransferTransportCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(true).withEvents(true)
                .withDescription("Setup Transfer Transport");
    }

    public HomekitSetupTransferTransportCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    protected byte[] encodeTLV8(Map<Integer, Object> value) {
        throw new UnsupportedOperationException("TLV8 encoding must be implemented for the specific device.");
    }

    @Override
    protected Map<Integer, Object> decodeTLV8(byte[] data) {
        throw new UnsupportedOperationException("TLV8 decoding must be implemented for the specific device.");
    }

    @Override
    public Map<Integer, Object> getDefault() {
        throw new UnsupportedOperationException("Default value must be implemented for the specific device.");
    }

    @Override
    public Map<Integer, Object> toValue(JsonValue jsonValue) {
        throw new UnsupportedOperationException("JSON to TLV8 conversion must be implemented for the specific device.");
    }

    @Override
    public Map<Integer, Object> toValue(State state) {
        throw new UnsupportedOperationException(
                "State to TLV8 conversion must be implemented for the specific device.");
    }

    @Override
    public State toState(Map<Integer, Object> value) {
        throw new UnsupportedOperationException(
                "TLV8 to State conversion must be implemented for the specific device.");
    }
}
