package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

@NonNullByDefault
@HomekitCharacteristicType(type = "000000D2-0000-1000-8000-0026BB765291", name = "Data", tag = "data")
public class HomekitBinaryDataCharacteristic extends org.openhab.io.homekit.core.characteristic.HomekitDataCharacteristic {
    public HomekitBinaryDataCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId)
            .withPairedWrite(true)
            .withPairedRead(true)
            .withEvents(true)
            .withDescription("Data");
    }

    public HomekitBinaryDataCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    // Optionally override encodeData/decodeData if needed
    // Optionally override toValue/toState if needed

    @Override
    public boolean isAllowedValue(byte[] value) {
        return true; // All binary data is allowed by default
    }

    @Override
    public java.util.Set<byte[]> getAllowedValues() {
        return java.util.Collections.emptySet(); // No specific allowed values
    }

    @Override
    public byte[] getDefault() {
        return new byte[0];
    }

    @Override
    public byte[] toValue(org.openhab.core.types.State state) {
        throw new UnsupportedOperationException("State to binary data conversion not implemented");
    }

    @Override
    public org.openhab.core.types.State toState(byte[] value) {
        throw new UnsupportedOperationException("Binary data to State conversion not implemented");
    }
} 