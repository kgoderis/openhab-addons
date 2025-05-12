package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitTLV8Characteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * App Matching Identifier Characteristic.
 */
@HomekitCharacteristicType(type = "000000A4-0000-1000-8000-0026BB765291", name = "App Matching Identifier", tag = "appMatchingIdentifier")
@NonNullByDefault
public class HomekitAppMatchingIdentifierCharacteristic extends HomekitTLV8Characteristic {

    public HomekitAppMatchingIdentifierCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId)
            .withPairedWrite(false)
            .withPairedRead(true)
            .withEvents(true)
            .withDescription("App Matching Identifier");
    }

    public HomekitAppMatchingIdentifierCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
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