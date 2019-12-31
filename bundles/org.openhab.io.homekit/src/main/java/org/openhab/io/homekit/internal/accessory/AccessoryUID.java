package org.openhab.io.homekit.internal.accessory;

import java.util.List;

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

    /**
     * Returns the id.
     *
     * @return id the id
     */
    public String getId() {
        List<String> segments = getAllSegments();
        return segments.get(segments.size() - 1);
    }
}
