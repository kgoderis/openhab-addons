package org.openhab.io.homekit.internal.service;

import java.util.List;

import org.openhab.core.thing.UID;

public class ServiceUID extends UID {

    // server id : accessory instance id : service id

    @Override
    protected int getMinimalNumberOfSegments() {
        return 3;
    }

    /**
     * Instantiates a new thing UID.
     *
     * @param serverId the server id
     * @param accessoryId the accessory instance id
     * @param instanceId the service instance id
     */
    public ServiceUID(String serverId, String accessoryId, String instanceId) {
        super(serverId, accessoryId, instanceId);
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
