package org.openhab.io.homekit.api.uid;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.Identifiable;

/**
 * Interface for HomeKit accessory server unique identifiers.
 * <p>
 * This interface defines the contract for accessory server UIDs in the HomeKit system. It provides
 * methods for accessing and managing unique identifiers for HomeKit accessory servers, ensuring
 * proper identification and security throughout the system.
 * </p>
 * <p>
 * The interface provides:
 * <ul>
 * <li>String representation of the UID</li>
 * <li>Pairing ID management</li>
 * <li>Unique identification</li>
 * </ul>
 * </p>
 * <p>
 * Key implementation details:
 * <ul>
 * <li>Thread-safe UID generation</li>
 * <li>Unique ID validation</li>
 * <li>Pairing ID association</li>
 * <li>String format consistency</li>
 * </ul>
 * </p>
 * <p>
 * The interface integrates with:
 * <ul>
 * <li>{@link org.openhab.core.common.registry.Identifiable} for registry integration</li>
 * <li>{@link org.openhab.io.homekit.api.server.HomekitAccessoryServer} for server identification</li>
 * </ul>
 * </p>
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
