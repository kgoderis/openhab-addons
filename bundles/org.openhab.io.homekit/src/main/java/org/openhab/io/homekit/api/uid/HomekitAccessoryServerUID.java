package org.openhab.io.homekit.api.uid;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.Identifiable;

/**
 * Interface for HomeKit accessory server unique identifiers.
 *
 * This interface defines the contract for accessory server UIDs in the HomeKit system. It provides
 * methods for accessing and managing unique identifiers for HomeKit accessory servers, ensuring
 * proper identification and security throughout the system.
 *
 * The interface provides:
 * - String representation of the UID
 * - Pairing ID management
 * - Unique identification
 *
 * Key implementation details:
 * - Thread-safe UID generation
 * - Unique ID validation
 * - Pairing ID association
 * - String format consistency
 *
 * The interface integrates with:
 * - {@link org.openhab.core.common.registry.Identifiable} for registry integration
 * - {@link org.openhab.io.homekit.api.server.HomekitAccessoryServer} for server identification
 *
 * @author Karel Goderis - Initial Contribution
 * @since 1.0.0
 */
@NonNullByDefault
public interface HomekitAccessoryServerUID extends Identifiable<HomekitAccessoryServerUID> {
    /**
     * Gets the unique identifier string for this accessory server.
     * This method provides a string representation of the server's unique identifier
     * that can be used for display, logging, and identification purposes.
     *
     * @return The unique identifier string
     * @since 1.0.0
     */
    String getAsString();

    /**
     * Gets the pairing ID.
     * This method retrieves the unique identifier used for pairing the server
     * with HomeKit clients.
     *
     * @return The pairing ID
     * @since 1.0.0
     */
    String getPairingId();
}
