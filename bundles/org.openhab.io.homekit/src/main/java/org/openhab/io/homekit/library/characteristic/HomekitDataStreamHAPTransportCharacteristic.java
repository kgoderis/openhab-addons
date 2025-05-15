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
 * HomeKit Data Stream HAP Transport Characteristic.
 * This characteristic represents the HAP transport configuration for data streaming.
 *
 * See the official HomeKit documentation for details.
 */
@HomekitCharacteristicType(type = "00000138-0000-1000-8000-0026BB765291", name = "Data Stream HAP Transport", tag = "dataStreamHAPTransport", acceptedItemTypes = {"String"})
@NonNullByDefault
public class HomekitDataStreamHAPTransportCharacteristic extends HomekitTLV8Characteristic {
    public HomekitDataStreamHAPTransportCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(true).withEvents(true)
                .withDescription("Data Stream HAP Transport");
    }

    public HomekitDataStreamHAPTransportCharacteristic(HomekitService service, HomekitEventManager eventManager,
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
