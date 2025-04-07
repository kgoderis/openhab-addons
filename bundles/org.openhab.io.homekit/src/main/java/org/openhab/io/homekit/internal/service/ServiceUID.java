package org.openhab.io.homekit.internal.service;

import org.openhab.core.thing.UID;

public class ServiceUID extends UID {

    // server id : accessory instance id : service id

    @Override
    protected int getMinimalNumberOfSegments() {
        return 5;
    }

    /**
     * Instantiates a new thing UID.
     *
     * @param hexId the the hexidecimal pre-generated id of the accessory
     * @param accessoryId the accessory instance id
     * @param serviceId the service instance id
     */
    public ServiceUID(String hexId, long accessoryId, long serviceId) {
        super("homekit", "service",hexId, Long.toString(accessoryId), Long.toString(serviceId));
    }

    // /**
    //  * Returns the id.
    //  *
    //  * @return id the id
    //  */
    // public String getId() {
    //     List<String> segments = getAllSegments();
    //     return segments.get(segments.size() - 1);
    // }
}
