package org.openhab.io.homekit.internal.characteristic;

import org.openhab.core.thing.UID;

public class CharacteristicUID extends UID {

    // server id : accessory instance id : service id : characteristic id

    @Override
    protected int getMinimalNumberOfSegments() {
        return 6;
    }

    /**
     * Instantiates a new thing UID.
     *
     * @param hexId the the hexidecimal pre-generated id of the accessory
     * @param accessoryId the accessory instance id
     * @param serviceId the accessory instance id
     * @param characteristicId the characteristic instance id
     */
    public CharacteristicUID(String hexId, long accessoryId, long serviceId, long characteristicId) {
        super("homekit", "characteristic", hexId, Long.toString(accessoryId), Long.toString(serviceId), Long.toString(characteristicId));
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
