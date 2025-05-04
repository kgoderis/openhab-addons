package org.openhab.io.homekit.internal.characteristic;

import org.openhab.io.homekit.internal.events.HomekitUID;

/**
 * Represents a unique identifier for a HomeKit characteristic.
 * The UID format is: homekit:characteristic:{pairingId}:{accessoryId}:{serviceId}:{characteristicId}
 */
public class CharacteristicUID extends HomekitUID {

    /**
     * Instantiates a new characteristic UID.
     *
     * @param pairingId the hexidecimal pre-generated id of the accessory
     * @param accessoryId the accessory instance id
     * @param serviceId the service instance id
     * @param characteristicId the characteristic instance id
     */
    public CharacteristicUID(String pairingId, long accessoryId, long serviceId, long characteristicId) {
        super("characteristic", "homekit:characteristic:" + pairingId + ":" + accessoryId + ":" + serviceId + ":" + characteristicId);
    }

    @Override
    protected int getMinimalNumberOfSegments() {
        return 6; // homekit:characteristic:pairingId:accessoryId:serviceId:characteristicId
    }

    /**
     * Returns the HomeKit ID part of this characteristic UID.
     * This is the last 4 segments joined with the separator.
     *
     * @return The HomeKit ID in the format: pairingId:accessoryId:serviceId:characteristicId
     */
    public String getHomekitId() {
        return String.join(SEPARATOR, getAllSegments().subList(getAllSegments().size() - 4, getAllSegments().size()));
    }
}
