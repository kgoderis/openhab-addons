package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitTLV8Characteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Pair Verify Characteristic.
 * @see <a href="https://developers.homebridge.io/#/characteristic/PairVerify">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "0000004E-0000-1000-8000-0026BB765291", name = "Pair Verify", tag = "pairVerify")
@NonNullByDefault
public class HomekitPairVerifyCharacteristic extends HomekitTLV8Characteristic {

    public HomekitPairVerifyCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId)
            .withPairedWrite(true)
            .withPairedRead(true)
            .withEvents(true)
            .withDescription("Pair Verify");
    }

    public HomekitPairVerifyCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
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