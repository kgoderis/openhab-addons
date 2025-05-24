package org.openhab.io.homekit.api.uid;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.Identifiable;

/**
 * Interface for HomeKit accessory server unique identifiers.
 * This interface defines the contract for accessory server UIDs in the HomeKit system.
 */
@NonNullByDefault
public interface HomekitAccessoryServerUID extends Identifiable<HomekitAccessoryServerUID> {
    /**
     * Gets the unique identifier string for this accessory server.
     *
     * @return the unique identifier string
     */
    String getAsString();

    /**
     * Gets the pairing ID.
     *
     * @return the pairing ID
     */
    String getPairingId();
} 