package org.openhab.io.homekit.api.uid;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.Identifiable;

/**
 * Interface for HomeKit pairing unique identifiers.
 * This interface defines the contract for pairing UIDs in the HomeKit system.
 */
@NonNullByDefault
public interface HomekitPairingUID extends Identifiable<HomekitPairingUID> {
    /**
     * Gets the unique identifier string for this pairing.
     *
     * @return the unique identifier string
     */
    String getAsString();

    /**
     * Gets the destination pairing ID.
     *
     * @return The destination pairing ID
     */
    byte[] getId();

    /**
     * Gets the source pairing ID.
     *
     * @return The source pairing ID
     */
    byte[] getSourcePairingId();
}
