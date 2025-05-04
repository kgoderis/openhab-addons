package org.openhab.io.homekit.internal.accessory;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.io.homekit.internal.events.HomekitUID;

/**
 * Represents a unique identifier for a HomeKit accessory.
 * The UID format is: homekit:accessory:{pairingId}:{accessoryId}
 */
public class AccessoryUID extends HomekitUID {

    /**
     * Creates a new AccessoryUID from an existing UID string.
     *
     * @param key the existing UID string
     */
    public AccessoryUID(@NonNull String key) {
        super("accessory", key);
    }

    @Override
    protected int getMinimalNumberOfSegments() {
        return 4; // homekit:accessory:pairingId:accessoryId
    }

    /**
     * Instantiates a new Accessory UID.
     *
     * @param pairingId the hexidecimal pre-generated id
     * @param accessoryId the accessory instance id
     */
    public AccessoryUID(String pairingId, long accessoryId) {
        super("accessory", "homekit:accessory:" + pairingId + ":" + accessoryId);
    }

    /**
     * Returns the pairing ID part of this accessory UID.
     *
     * @return The pairing ID
     */
    public String getPairingId() {
        return getSegment(2);
    }
}
