package org.openhab.io.homekit.api.uid;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.Identifiable;

/**
 * Interface for HomeKit service unique identifiers.
 * This interface defines the contract for service UIDs in the HomeKit system.
 */
@NonNullByDefault
public interface HomekitServiceUID extends Identifiable<HomekitServiceUID> {
    /**
     * Gets the unique identifier string for this service.
     *
     * @return the unique identifier string
     */
    String getAsString();

    /**
     * Gets the service instance ID.
     *
     * @return the service instance ID
     */
    long getInstanceId();
}
