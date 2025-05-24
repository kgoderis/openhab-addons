package org.openhab.io.homekit.core.server;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.uid.HomekitAccessoryServerUID;
import org.openhab.io.homekit.util.HomekitUID;

/**
 * Represents a unique identifier for a Homekit accessory server.
 * The UID format is: homekit:server:{pairingId}
 */
@NonNullByDefault
public class HomekitAccessoryServerUIDImpl extends HomekitUID implements HomekitAccessoryServerUID {
    private static final String SERVER_PREFIX = "server";

    /**
     * Default constructor in package scope only. Will allow to instantiate this
     * class by reflection. Not intended to be used for normal instantiation.
     */
    HomekitAccessoryServerUIDImpl() {
        super(SERVER_PREFIX, "homekit:" + SERVER_PREFIX + ":");
    }

    /**
     * Instantiates a new accessory server UID.
     *
     * @param pairingId the pairing ID of the accessory server
     */
    public HomekitAccessoryServerUIDImpl(String pairingId) {
        super(SERVER_PREFIX, "homekit:" + SERVER_PREFIX + ":" + pairingId);
    }

    @Override
    public String getAsString() {
        return toString();
    }

    @Override
    public String getPairingId() {
        return getSegment(2);
    }

    @Override
    protected int getMinimalNumberOfSegments() {
        return 3; // homekit:server:pairingId
    }

    @Override
    public HomekitAccessoryServerUID getUID() {
        return this;
    }
} 