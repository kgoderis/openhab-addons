package org.openhab.io.homekit.internal.service;

import org.openhab.io.homekit.internal.events.HomekitUID;

/**
 * Represents a unique identifier for a HomeKit service.
 * The UID format is: homekit:service:{pairingId}:{accessoryId}:{serviceId}
 */
public class HomekitServiceUID extends HomekitUID {

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
    public HomekitServiceUID(String pairingId, long accessoryId, long serviceId) {
        super("service", "homekit:service:" + pairingId + ":" + accessoryId + ":" + serviceId);
    }

    /**
     * Returns the service ID part of this service UID.
     *
     * @return The service ID
     */
    public String getServiceId() {
        return getSegment(4);
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
