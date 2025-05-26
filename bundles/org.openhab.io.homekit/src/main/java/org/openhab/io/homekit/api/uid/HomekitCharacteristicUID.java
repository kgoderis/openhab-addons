package org.openhab.io.homekit.api.uid;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.Identifiable;

/**
 * Interface for HomeKit characteristic unique identifiers.
 * This interface defines the contract for characteristic UIDs in the HomeKit system.
 */
@NonNullByDefault
public interface HomekitCharacteristicUID extends Identifiable<HomekitCharacteristicUID> {
    /**
     * Gets the unique identifier string for this characteristic.
     *
     * @return the unique identifier string
     */
    String getAsString();

    /**
     * Gets the characteristic instance ID.
     *
     * @return the characteristic instance ID
     */
    long getInstanceId();
}
