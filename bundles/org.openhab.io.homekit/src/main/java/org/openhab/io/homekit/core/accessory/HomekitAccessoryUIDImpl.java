package org.openhab.io.homekit.core.accessory;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.uid.HomekitAccessoryUID;
import org.openhab.io.homekit.util.HomekitUID;

/**
 * Represents a unique identifier for a Homekit accessory.
 * The UID format is: homekit:accessory:{pairingId}:{accessoryId}
 */
@NonNullByDefault
public class HomekitAccessoryUIDImpl extends HomekitUID implements HomekitAccessoryUID {
    private final long accessoryId;

    /**
     * Creates a new HomekitAccessoryUID from an existing UID string.
     *
     * @param key the existing UID string
     */
    public HomekitAccessoryUIDImpl(String key) {
        super("accessory", key);
        this.accessoryId = Long.parseLong(getSegment(4));
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
    public HomekitAccessoryUIDImpl(String pairingId, long accessoryId) {
        super("accessory", "homekit:accessory:" + pairingId + ":" + accessoryId);
        this.accessoryId = accessoryId;
    }

    /**
     * Returns the pairing ID part of this accessory UID.
     *
     * @return The pairing ID
     */
    @Override
    public String getPairingId() {
        return getSegment(2);
    }

    @Override
    public String getAsString() {
        return toString();
    }

    @Override
    public long getAccessoryId() {
        return accessoryId;
    }

    @Override
    public HomekitAccessoryUID getUID() {
        return this;
    }
}
