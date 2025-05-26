package org.openhab.io.homekit.core.server;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.uid.HomekitAccessoryServerUID;
import org.openhab.io.homekit.util.HomekitUID;

/**
 * Implementation of a unique identifier for a HomeKit accessory server.
 *
 * <p>
 * This class provides a structured way to identify HomeKit accessory servers within the system.
 * The UID format follows the pattern: homekit:server:{pairingId}
 * where:
 * <ul>
 *   <li>pairingId: The unique identifier for the server's pairing</li>
 * </ul>
 * </p>
 *
 * <p>
 * The class integrates with:
 * <ul>
 *   <li>{@link HomekitUID} for base UID functionality</li>
 *   <li>{@link HomekitAccessoryServerUID} for server-specific UID operations</li>
 *   <li>{@link org.openhab.core.thing.UID OpenHAB's UID system} for unique identification</li>
 * </ul>
 * </p>
 *
 * @author Karel Goderis - Initial contribution
 * @version 1.0
 * @since 1.0
 */
@NonNullByDefault
public class HomekitAccessoryServerUIDImpl extends HomekitUID implements HomekitAccessoryServerUID {
    private static final String SERVER_PREFIX = "server";

    /**
     * Creates a new server UID with default values.
     * This constructor is package-private and intended for use by reflection only.
     * Not meant for normal instantiation.
     */
    HomekitAccessoryServerUIDImpl() {
        super(SERVER_PREFIX, "homekit:" + SERVER_PREFIX + ":");
    }

    /**
     * Creates a new server UID with the specified pairing ID.
     *
     * This constructor builds a complete server UID from its pairing ID.
     * The resulting UID will be in the format: homekit:server:{pairingId}
     *
     * @param pairingId The unique identifier for the server's pairing
     */
    public HomekitAccessoryServerUIDImpl(String pairingId) {
        super(SERVER_PREFIX, "homekit:" + SERVER_PREFIX + ":" + pairingId);
    }

    /**
     * Gets the complete UID as a string.
     *
     * @return The UID string representation
     */
    @Override
    public String getAsString() {
        return toString();
    }

    /**
     * Gets the pairing ID component of this UID.
     *
     * This method extracts the pairing ID from the UID segments.
     * The pairing ID is the third segment in the UID string.
     *
     * @return The pairing ID as a string
     */
    @Override
    public String getPairingId() {
        return getSegment(2);
    }

    /**
     * Gets the minimum number of segments required for a valid UID.
     *
     * @return The minimum number of segments (3 for homekit:server:pairingId)
     */
    @Override
    protected int getMinimalNumberOfSegments() {
        return 3; // homekit:server:pairingId
    }

    /**
     * Gets this UID instance.
     *
     * @return This UID instance
     */
    @Override
    public HomekitAccessoryServerUID getUID() {
        return this;
    }
}
