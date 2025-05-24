package org.openhab.io.homekit.core.event;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.uid.HomekitPeerGroupUID;
import org.openhab.io.homekit.util.HomekitUID;

/**
 * Represents a unique identifier for a Homekit peer group.
 * The UID format is: homekit:peer:{peerGroup}
 */
@NonNullByDefault
public class HomekitPeerGroupUIDImpl extends HomekitUID implements HomekitPeerGroupUID {
    private static final String PEER_PREFIX = "peer";
    private final String peerGroup;

    /**
     * Creates a new HomekitPeerGroupUID with the specified peer group.
     * The actual UID will be in the format: homekit:peer:{peerGroup}
     *
     * @param peerGroup The peer group this UID represents
     */
    public HomekitPeerGroupUIDImpl(String peerGroup) {
        super(PEER_PREFIX, "homekit:" + PEER_PREFIX + ":" + peerGroup);
        this.peerGroup = peerGroup;
    }

    @Override
    public String getAsString() {
        return toString();
    }

    @Override
    public String getPeerGroup() {
        return peerGroup;
    }

    @Override
    protected int getMinimalNumberOfSegments() {
        return 3; // homekit:peer:peerGroup
    }

    @Override
    public HomekitPeerGroupUID getUID() {
        return this;
    }
} 