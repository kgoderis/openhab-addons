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
 * HomeKit Supported Video Stream Configuration Characteristic.
 * This characteristic represents the supported video stream configuration for a device.
 *
 * See the official HomeKit documentation for details.
 */
@HomekitCharacteristicType(type = "00000114-0000-1000-8000-0026BB765291", name = "Supported Video Stream Configuration", tag = "supportedVideoStreamConfiguration")
@NonNullByDefault
public class HomekitSupportedVideoStreamConfigurationCharacteristic extends HomekitTLV8Characteristic {
    public HomekitSupportedVideoStreamConfigurationCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId)
            .withPairedRead(true)
            .withDescription("Supported Video Stream Configuration");
    }

    public HomekitSupportedVideoStreamConfigurationCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
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
        throw new UnsupportedOperationException("State to TLV8 conversion must be implemented for the specific device.");
    }

    @Override
    public State toState(Map<Integer, Object> value) {
        throw new UnsupportedOperationException("TLV8 to State conversion must be implemented for the specific device.");
    }
} 