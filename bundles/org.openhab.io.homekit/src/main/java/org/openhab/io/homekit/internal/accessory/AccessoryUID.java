package org.openhab.io.homekit.internal.accessory;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.core.thing.UID;

public class AccessoryUID extends UID {

    // server id : accessory instance id

    public AccessoryUID(@NonNull String key) {
        super(key);
    }

    @Override
    protected int getMinimalNumberOfSegments() {
        return 4;
    }

    /**
     * Instantiates a new Accessory UID.
     *
     * @param pairingId the hexidecimal pre-generated id
     * @param instanceId the accessory instance id
     */
    public AccessoryUID(String pairingId, long accessoryId) {
        super("homekit", "accessory", pairingId, Long.toString(accessoryId));
    }

    public String getPairingId() {
        return getSegment(2);
    }
}
