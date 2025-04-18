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
     * @param pairingId the the hexidecimal pre-generated id of the accessory
     * @param accessoryId the accessory instance id
     * @param serviceId the accessory instance id
     * @param characteristicId the characteristic instance id
     */
    public CharacteristicUID(String pairingId, long accessoryId, long serviceId, long characteristicId) {
        super("homekit", "characteristic", pairingId, Long.toString(accessoryId), Long.toString(serviceId),
                Long.toString(characteristicId));
    }

    // return the last 4 segments as a string with SEPARATOR as a separator
    public String getHomekitId() {
        return String.join(SEPARATOR, getAllSegments().subList(getAllSegments().size() - 4, getAllSegments().size()));
    }
}
