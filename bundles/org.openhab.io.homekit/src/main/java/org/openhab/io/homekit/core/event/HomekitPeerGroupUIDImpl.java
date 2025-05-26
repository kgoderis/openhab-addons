package org.openhab.io.homekit.core.event;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.uid.HomekitPeerGroupUID;
import org.openhab.io.homekit.util.HomekitUID;

/**
 * Implementation of a unique identifier for a HomeKit peer group.
 *
 * <p>
 * This class provides a structured way to identify HomeKit peer groups within the system.
 * The UID format follows the pattern: homekit:peer:{peerGroup}
 * where:
 * <ul>
 *   <li>peerGroup: The identifier for the group of peers</li>
 * </ul>
 * </p>
 *
 * <p>
 * The class integrates with:
 * <ul>
 *   <li>{@link HomekitUID} for base UID functionality</li>
 *   <li>{@link HomekitPeerGroupUID} for peer group-specific UID operations</li>
 *   <li>{@link org.openhab.core.thing.UID OpenHAB's UID system} for unique identification</li>
 * </ul>
 * </p>
 *
 * @author Karel Goderis - Initial contribution
 * @version 1.0
 * @since 1.0
 */
@NonNullByDefault
public class HomekitPeerGroupUIDImpl extends HomekitUID implements HomekitPeerGroupUID {
    private static final String PEER_PREFIX = "peer";
    private final String peerGroup;

    /**
     * Creates a new peer group UID with the specified group identifier.
     *
     * This constructor builds a complete peer group UID from its group identifier.
     * The resulting UID will be in the format: homekit:peer:{peerGroup}
     *
     * @param peerGroup The identifier for the group of peers
     */
    public HomekitPeerGroupUIDImpl(String peerGroup) {
        super(PEER_PREFIX, "homekit:" + PEER_PREFIX + ":" + peerGroup);
        this.peerGroup = peerGroup;
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
     * Gets the peer group identifier.
     *
     * @return The peer group identifier
     */
    @Override
    public String getPeerGroup() {
        return peerGroup;
    }

    /**
     * Gets the minimum number of segments required for a valid UID.
     *
     * @return The minimum number of segments (3 for homekit:peer:peerGroup)
     */
    @Override
    protected int getMinimalNumberOfSegments() {
        return 3; // homekit:peer:peerGroup
    }

    /**
     * Gets this UID instance.
     *
     * @return This UID instance
     */
    @Override
    public HomekitPeerGroupUID getUID() {
        return this;
    }
}
