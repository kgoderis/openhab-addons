package org.openhab.io.homekit.api.uid;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.Identifiable;

/**
 * Interface for HomeKit service unique identifiers.
 * <p>
 * This interface defines the contract for service UIDs in the HomeKit system.
 * It provides
 * methods for accessing and managing unique identifiers for HomeKit services,
 * ensuring
 * proper identification and tracking throughout the system.
 * </p>
 * <p>
 * The interface provides:
 * <ul>
 * <li>String representation of the UID</li>
 * <li>Instance ID management</li>
 * <li>Unique identification</li>
 * </ul>
 * </p>
 * <p>
 * Key implementation details:
 * <ul>
 * <li>Thread-safe UID generation</li>
 * <li>Unique ID validation</li>
 * <li>Instance ID association</li>
 * <li>String format consistency</li>
 * </ul>
 * </p>
 * <p>
 * The interface integrates with:
 * <ul>
 * <li>{@link org.openhab.core.common.registry.Identifiable} for registry
 * integration</li>
 * <li>{@link org.openhab.io.homekit.api.service.HomekitService} for service
 * identification</li>
 * </ul>
 * </p>
 *
 * @author Karel Goderis - Initial Contribution
 * @since 1.0.0
 */
@NonNullByDefault
public interface HomekitServiceUID extends Identifiable<HomekitServiceUID> {
    /**
     * Gets the unique identifier string for this service.
     * This method provides a string representation of the service's unique
     * identifier
     * that can be used for display, logging, and identification purposes.
     *
     * @return The unique identifier string
     * @since 1.0.0
     */
    String toString();

    /**
     * Gets the service instance ID.
     * This method retrieves the numeric identifier assigned to the service,
     * which is used for internal tracking and management.
     *
     * @return The service instance ID
     * @since 1.0.0
     */
    long getInstanceId();
}
