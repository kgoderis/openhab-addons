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
 * Security considerations:
 * - All cryptographic data is stored as byte arrays
 * - No direct access to private keys is provided
 * - Public key exchange is supported for secure communication
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
     * accessory/server side of the pairing relationship. The source identifier
     * is a unique cryptographic identifier that ensures secure communication
     * between the accessory and the iOS device.
     *
     * @return The source identifier as a byte array
     * @throws IllegalStateException if the source identifier is not available
     */
    public byte[] getSourceId() throws IllegalStateException;

    /**
     * Gets the destination identifier for this pairing.
     *
     * This method retrieves the destination identifier, which represents the
     * client/controller side of the pairing relationship. The destination identifier
     * is used to uniquely identify the iOS device in the pairing relationship.
     *
     * @return The destination identifier as a byte array
     * @throws IllegalStateException if the destination identifier is not available
     */
    public byte[] getDestinationId() throws IllegalStateException;

    /**
     * Gets the public key for this pairing.
     *
     * This method retrieves the public key used for secure communication
     * between the accessory and the iOS device. The public key is essential
     * for establishing encrypted communication channels and verifying message
     * authenticity.
     *
     * @return The public key as a byte array
     * @throws IllegalStateException if the public key is not available
     */
    public byte[] getPublicKey() throws IllegalStateException;
}
