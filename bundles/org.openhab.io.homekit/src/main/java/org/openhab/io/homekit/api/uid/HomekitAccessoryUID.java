package org.openhab.io.homekit.api.uid;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.Identifiable;

/**
 * Interface for HomeKit accessory unique identifiers.
 * This interface defines the contract for accessory UIDs in the HomeKit system.
 */
@NonNullByDefault
public interface HomekitAccessoryUID extends Identifiable<HomekitAccessoryUID> {
    /**
     * Gets the unique identifier string for this accessory.
     *
     * @return the unique identifier string
     */
    String getAsString();

    /**
     * Gets the accessory ID.
     *
     * @return the accessory ID
     */
    long getAccessoryId();

    /**
     * Gets the pairing ID.
     *
     * @return the pairing ID
     */
    String getPairingId();
} 