package org.openhab.io.homekit.core.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

@NonNullByDefault
public abstract class HomekitDataCharacteristic extends AbstractHomekitCharacteristic<byte[]> {
    public HomekitDataCharacteristic(HomekitService service, HomekitEventManager eventManager) {
        super(service, eventManager);
        withFormat("data").withPairedWrite(true).withPairedRead(true).withEvents(true);
        initializeValue();
    }

    public HomekitDataCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
        initializeValue();
    }

    @Override
    public boolean isHidden() {
        return false;
    }

    /**
     * Encode the byte[] data for storage or transmission (default: identity).
     */
    protected byte[] encodeData(byte[] value) {
        return value;
    }

    /**
     * Decode the byte[] data (default: identity).
     */
    protected byte[] decodeData(byte[] data) {
        return data;
    }

    @Override
    public byte[] toValue(JsonValue jsonValue) {
        // Implementers should override this for custom data JSON handling
        throw new UnsupportedOperationException("Data JSON conversion not implemented");
    }

    @Override
    public JsonValue toValueJson(byte[] value) {
        // Implementers should override this for custom data JSON handling
        throw new UnsupportedOperationException("Data JSON conversion not implemented");
    }
} 