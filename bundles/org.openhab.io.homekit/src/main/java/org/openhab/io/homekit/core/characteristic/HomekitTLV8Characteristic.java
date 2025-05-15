package org.openhab.io.homekit.core.characteristic;

import java.util.Map;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

@NonNullByDefault
public abstract class HomekitTLV8Characteristic extends AbstractHomekitCharacteristic<Map<Integer, Object>> {
    public HomekitTLV8Characteristic(HomekitService service, HomekitEventManager eventManager) {
        super(service, eventManager);
        withFormat("tlv8").withPairedWrite(true).withPairedRead(true).withEvents(true);
        initializeValue();
    }

    public HomekitTLV8Characteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
        initializeValue();
    }

    @Override
    public boolean isHidden() {
        return false;
    }

    /**
     * Encode the TLV8 map to a byte array for storage or transmission.
     */
    protected abstract byte[] encodeTLV8(Map<Integer, Object> value);

    /**
     * Decode a byte array into a TLV8 map.
     */
    protected abstract Map<Integer, Object> decodeTLV8(byte[] data);

    @Override
    public Map<Integer, Object> toValue(JsonValue jsonValue) {
        // Implementers should override this for custom TLV8 JSON handling
        throw new UnsupportedOperationException("TLV8 JSON conversion not implemented");
    }

    @Override
    public JsonValue toValueJson(Map<Integer, Object> value) {
        // Implementers should override this for custom TLV8 JSON handling
        throw new UnsupportedOperationException("TLV8 JSON conversion not implemented");
    }
}
