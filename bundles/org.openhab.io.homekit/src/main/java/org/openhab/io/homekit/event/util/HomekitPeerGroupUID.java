package org.openhab.io.homekit.event.util;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.util.HomekitUID;

/**
 * Implementation of {@link HomekitPeerGroupUID} for generating and managing unique identifiers
 * for HomeKit peer groups. This class extends {@link HomekitUID} to provide specialized UID
 * handling for peer groups in the HomeKit event system.
 *
 * Peer groups are used to organize and manage related HomeKit components that need to
 * communicate with each other. The UID format follows the pattern: homekit:peer:{peerGroup}
 *
 * The class integrates with:
 * - {@link HomekitUID} for base UID functionality
 * - {@link org.openhab.core.thing.UID OpenHAB's UID system} for compatibility
 *
 * UID Structure:
 * - Format: homekit:peer:{peerGroup}
 * - Prefix: "homekit" - Identifies the HomeKit domain
 * - Category: "peer" - Specifies the peer group category
 * - Peer Group: Custom identifier for grouping related components
 *
 * Key Features:
 * - Peer group identification
 * - Consistent UID format
 * - Group-based component organization
 * - Format validation
 * - Type safety
 *
 * Usage Patterns:
 * - Creating peer groups: Use {@link #HomekitPeerGroupUID(String)}
 * - Retrieving group identifier: Use {@link #getPeerGroup()}
 * - Grouping related components
 * - Managing component relationships
 * - Organizing event routing
 *
 * Validation Rules:
 * - UID must start with "homekit:peer:"
 * - Peer group identifier must not be null or empty
 * - Minimum of 3 segments required
 * - Peer group format must be valid
 *
 * @author Karel Goderis - Initial contribution
 */
@NonNullByDefault
public class HomekitPeerGroupUID extends HomekitUID {
    private static final String PEER_PREFIX = "peer";
    private final String peerGroup;

    /**
     * Creates a new HomekitPeerGroupUID with the specified peer group.
     * The actual UID will be in the format: homekit:peer:{peerGroup}
     *
     * @param peerGroup The peer group this UID represents
     * @throws IllegalArgumentException if peerGroup is null or empty
     */
    public HomekitPeerGroupUID(String peerGroup) {
        super(PEER_PREFIX, peerGroup);
        this.peerGroup = peerGroup;
    }

    /**
     * Returns the peer group identifier of this UID.
     * This is the unique identifier that groups related HomeKit components together.
     *
     * @return The peer group identifier
     */
    public String getPeerGroup() {
        return peerGroup;
    }

    /**
     * Specifies the minimum number of segments required in a valid peer group UID.
     * The format is: homekit:peer:peerGroup
     *
     * @return The minimum number of segments (3)
     */
    @Override
    protected int getMinimalNumberOfSegments() {
        return 3; // homekit:peer:peerGroup
    }
}
