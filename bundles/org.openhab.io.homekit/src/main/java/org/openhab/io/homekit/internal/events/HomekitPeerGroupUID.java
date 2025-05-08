package org.openhab.io.homekit.internal.events;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.UID;

/**
 * Represents a unique identifier for a HomeKit peer group.
 * The UID format is: homekit:peer:{peerGroup}
 */
@NonNullByDefault
public class HomekitPeerGroupUID extends UID {
    private static final String HOMEKIT_PREFIX = "homekit";
    private static final String PEER_PREFIX = "peer";
    private final String peerGroup;

    /**
     * Creates a new HomekitPeerGroupUID with the specified peer group.
     * The actual UID will be in the format: homekit:peer:{peerGroup}
     *
     * @param peerGroup The peer group this UID represents
     */
    public HomekitPeerGroupUID(String peerGroup) {
        super(HOMEKIT_PREFIX, PEER_PREFIX, peerGroup);
        this.peerGroup = peerGroup;
    }

    /**
     * Returns the peer group of this UID.
     *
     * @return The peer group
     */
    public String getPeerGroup() {
        return peerGroup;
    }

    @Override
    protected int getMinimalNumberOfSegments() {
        return 3; // homekit:peer:peerGroup
    }
}
