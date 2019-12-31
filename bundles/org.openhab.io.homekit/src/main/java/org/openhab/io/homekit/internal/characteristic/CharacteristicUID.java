package org.openhab.io.homekit.internal.characteristic;

import java.util.List;

import org.openhab.core.thing.UID;

public class CharacteristicUID extends UID {

    // server id : accessory instance id : service id : characteristic id

    @Override
    protected int getMinimalNumberOfSegments() {
        return 4;
    }

    /**
     * Instantiates a new thing UID.
     *
     * @param serverId the server id
     * @param accessoryId the accessory instance id
     * @param serviceId the accessory instance id
     * @param characteristicId the characteristic instance id
     */
    public CharacteristicUID(String serverId, long accessoryId, long serviceId, long characteristicId) {
        super(serverId, Long.toString(accessoryId), Long.toString(serviceId), Long.toString(characteristicId));
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
