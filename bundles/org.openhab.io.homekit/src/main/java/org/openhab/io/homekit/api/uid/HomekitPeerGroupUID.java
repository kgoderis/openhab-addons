package org.openhab.io.homekit.api.uid;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.Identifiable;

/**
 * Interface for HomeKit peer group unique identifiers.
 * This interface defines the contract for peer group UIDs in the HomeKit system.
 */
@NonNullByDefault
public interface HomekitPeerGroupUID extends Identifiable<HomekitPeerGroupUID> {
    /**
     * Gets the unique identifier string for this peer group.
     *
     * @return the unique identifier string
     */
    String getAsString();

    /**
     * Gets the peer group name.
     *
     * @return the peer group name
     */
    String getPeerGroup();
}
