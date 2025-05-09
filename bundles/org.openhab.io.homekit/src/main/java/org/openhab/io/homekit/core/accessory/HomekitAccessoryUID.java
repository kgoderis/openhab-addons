package org.openhab.io.homekit.core.accessory;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.io.homekit.util.HomekitUID;

/**
 * Represents a unique identifier for a Homekit accessory.
 * The UID format is: homekit:accessory:{pairingId}:{accessoryId}
 */
public class HomekitAccessoryUID extends HomekitUID {

    /**
     * Creates a new HomekitAccessoryUID from an existing UID string.
     *
     * @param key the existing UID string
     */
    public HomekitAccessoryUID(@NonNull String key) {
        super("accessory", key);
    }

    @Override
    protected int getMinimalNumberOfSegments() {
        return 4; // homekit:accessory:pairingId:accessoryId
    }

    /**
     * Instantiates a new HomekitAccessory UID.
     *
     * @param pairingId the hexidecimal pre-generated id
     * @param accessoryId the accessory instance id
     */
    public HomekitAccessoryUID(String pairingId, long accessoryId) {
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
