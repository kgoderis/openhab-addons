package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitTLV8Characteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Relay Control Point Characteristic.
 * @see <a href="https://developers.homebridge.io/#/characteristic/RelayControlPoint">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "0000005E-0000-1000-8000-0026BB765291", name = "Relay Control Point", tag = "relayControlPoint")
@NonNullByDefault
public class HomekitRelayControlPointCharacteristic extends HomekitTLV8Characteristic {

    public HomekitRelayControlPointCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId)
            .withPairedWrite(true)
            .withPairedRead(true)
            .withEvents(true)
            .withDescription("Relay Control Point");
    }

    public HomekitRelayControlPointCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    protected byte[] encodeTLV8(java.util.Map<Integer, Object> value) {
        return new byte[0];
    }

    @Override
    protected java.util.Map<Integer, Object> decodeTLV8(byte[] data) {
        return java.util.Collections.emptyMap();
    }

    @Override
    public java.util.Map<Integer, Object> getDefault() {
        return java.util.Collections.emptyMap();
    }

    @Override
    public java.util.Map<Integer, Object> toValue(org.openhab.core.types.State state) {
        return java.util.Collections.emptyMap();
    }

    @Override
    public org.openhab.core.types.State toState(java.util.Map<Integer, Object> value) {
        return null;
    }
} 