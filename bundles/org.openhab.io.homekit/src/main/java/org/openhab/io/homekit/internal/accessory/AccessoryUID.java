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
        return 2;
    }

    /**
     * Instantiates a new Accessory UID.
     *
     * @param serverId the server id
     * @param instanceId the accessory instance id
     */
    public AccessoryUID(String serverId, String instanceId) {
        super(serverId, instanceId);
    }

}
