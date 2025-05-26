package org.openhab.io.homekit.protocol.pairing;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.Identifiable;
import org.openhab.io.homekit.api.uid.HomekitPairingUID;

/**
 * Defines the contract for HomeKit pairing implementations.
 *
 * This interface represents a HomeKit pairing relationship between an accessory
 * and an iOS device. It extends {@link Identifiable} to provide unique identification
 * through {@link HomekitPairingUID}.
 *
 * The interface works in conjunction with:
 * - {@link HomekitPairingUID} for unique identification
 * - {@link HomekitPairingImpl} for concrete implementations
 * - {@link HomekitPairingRegistry} for pairing management
 *
 * Key responsibilities:
 * 1. Providing access to pairing identifiers
 * 2. Managing cryptographic keys
 * 3. Supporting secure communication
 *
 * The implementation uses byte arrays for storing cryptographic identifiers
 * and keys, which are essential for the HomeKit security model.
 *
 * @author Karel Goderis - Initial Contribution
 * @since 1.0
 */
@NonNullByDefault
public interface HomekitPairing extends Identifiable<HomekitPairingUID> {

    /**
     * Gets the source identifier for this pairing.
     *
     * This method retrieves the source identifier, which represents the
     * accessory/server side of the pairing relationship.
     *
     * @return The source identifier as a byte array
     */
    public byte[] getSourceId();

    /**
     * Gets the destination identifier for this pairing.
     *
     * This method retrieves the destination identifier, which represents the
     * client/controller side of the pairing relationship.
     *
     * @return The destination identifier as a byte array
     */
    public byte[] getDestinationId();

    /**
     * Gets the public key for this pairing.
     *
     * This method retrieves the public key used for secure communication
     * between the accessory and the iOS device.
     *
     * @return The public key as a byte array
     */
    public byte[] getPublicKey();
}
