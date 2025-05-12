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
 * This characteristic represents the supported configuration for access codes.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/AccessCodeSupportedConfiguration">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "00000261-0000-1000-8000-0026BB765291", name = "Access Code Supported Configuration", tag = "accessCodeSupportedConfiguration")
@NonNullByDefault
public class HomekitAccessCodeSupportedConfigurationCharacteristic extends HomekitTLV8Characteristic {
    public HomekitAccessCodeSupportedConfigurationCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId)
            .withPairedWrite(false)
            .withPairedRead(true)
            .withEvents(false)
            .withDescription("Access Code Supported Configuration");
    }

    public HomekitAccessCodeSupportedConfigurationCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    protected byte[] encodeTLV8(Map<Integer, Object> value) {
        throw new UnsupportedOperationException("Encoding not implemented");
    }

    @Override
    protected Map<Integer, Object> decodeTLV8(byte[] data) {
        throw new UnsupportedOperationException("Decoding not implemented");
    }

    @Override
    public Map<Integer, Object> getDefault() {
        throw new UnsupportedOperationException("Default value not implemented");
    }

    @Override
    public Map<Integer, Object> toValue(State state) {
        throw new UnsupportedOperationException("State to TLV8 not implemented");
    }

    @Override
    public State toState(Map<Integer, Object> value) {
        throw new UnsupportedOperationException("TLV8 to State not implemented");
    }
} 