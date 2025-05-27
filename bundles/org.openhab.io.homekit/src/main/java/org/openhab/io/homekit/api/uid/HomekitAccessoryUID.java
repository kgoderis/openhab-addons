package org.openhab.io.homekit.api.uid;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.Identifiable;

/**
 * Interface for HomeKit accessory unique identifiers.
 *
 * This interface defines the contract for accessory UIDs in the HomeKit system. It provides
 * methods for accessing and managing unique identifiers for HomeKit accessories, ensuring
 * proper identification and tracking throughout the system.
 *
 * The interface provides:
 * - String representation of the UID
 * - Accessory ID retrieval
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
 * - {@link org.openhab.io.homekit.api.accessory.HomekitAccessory} for accessory identification
 *
 * @author Karel Goderis - Initial Contribution
 * @since 1.0.0
 */
@NonNullByDefault
public interface HomekitAccessoryUID extends Identifiable<HomekitAccessoryUID> {
    /**
     * Gets the unique identifier string for this accessory.
     * This method provides a string representation of the accessory's unique identifier
     * that can be used for display, logging, and identification purposes.
     *
     * @return The unique identifier string
     * @since 1.0.0
     */
    String getAsString();

    /**
     * Gets the accessory ID.
     * This method retrieves the numeric identifier assigned to the accessory,
     * which is used for internal tracking and management.
     *
     * @return The accessory ID
     * @since 1.0.0
     */
    long getAccessoryId();

    /**
     * Gets the pairing ID.
     * This method retrieves the unique identifier used for pairing the accessory
     * with HomeKit clients.
     *
     * @return The pairing ID
     * @since 1.0.0
     */
    String getPairingId();
}
