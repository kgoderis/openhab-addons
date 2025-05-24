package org.openhab.io.homekit.core.service;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.uid.HomekitServiceUID;
import org.openhab.io.homekit.util.HomekitUID;

/**
 * Represents a unique identifier for a Homekit service.
 * The UID format is: homekit:service:{pairingId}:{accessoryId}:{serviceId}
 */
@NonNullByDefault
public class HomekitServiceUIDImpl extends HomekitUID implements HomekitServiceUID {
    private final long instanceId;

    // server id : accessory instance id : service id

    @Override
    protected int getMinimalNumberOfSegments() {
        return 5; // homekit:service:pairingId:accessoryId:serviceId
    }

    /**
     * Instantiates a new service UID.
     *
     * @param pairingId the hexidecimal pre-generated id of the accessory
     * @param accessoryId the accessory instance id
     * @param serviceId the service instance id
     */
    public HomekitServiceUIDImpl(String pairingId, long accessoryId, long serviceId) {
        super("service", "homekit:service:" + pairingId + ":" + accessoryId + ":" + serviceId);
        this.instanceId = accessoryId;
    }

    public HomekitServiceUIDImpl(String key) {
        super("service", key);
        this.instanceId = 0;
    }

    /**
     * Returns the service ID part of this service UID.
     *
     * @return The service ID
     */
    public String getServiceId() {
        return getSegment(4);
    }

    @Override
    public String getAsString() {
        return toString();
    }

    @Override
    public long getInstanceId() {
        return instanceId;
    }

    @Override
    public HomekitServiceUID getUID() {
        return this;
    }

    // /**
    // * Returns the id.
    // *
    // * @return id the id
    // */
    // public String getId() {
    // List<String> segments = getAllSegments();
    // return segments.get(segments.size() - 1);
    // }
}
