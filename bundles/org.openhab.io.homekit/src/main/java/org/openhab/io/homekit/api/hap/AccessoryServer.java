package org.openhab.io.homekit.api.hap;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Collection;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.Identifiable;
import org.openhab.io.homekit.api.listener.AccessoryServerChangeListener;
import org.openhab.io.homekit.exception.HomekitAccessoryOperationException;
import org.openhab.io.homekit.exception.HomekitConfigurationException;
import org.openhab.io.homekit.exception.HomekitEventException;
import org.openhab.io.homekit.exception.HomekitException;
import org.openhab.io.homekit.exception.HomekitServerException;
import org.openhab.io.homekit.internal.server.AccessoryServerUID;

/**
 * Interface for an HAP Accessory Server that exposes a collection of Accessories to the HAP controller(s).
 * An HAP Accessory Server represents one endpoint of the pairing relationship established with HAP Pairing.
 * This interface provides methods for managing accessories, handling pairing, and controlling server behavior.
 *
 * @author Karel Goderis
 */
@NonNullByDefault
public interface AccessoryServer extends Identifiable<AccessoryServerUID> {

    // ==================== Core Server Methods ====================

    /**
     * Gets the unique identifier for this accessory server.
     * This ID is used for advertising and pairing purposes.
     *
     * @return the unique server identifier
     */
    @Override
    AccessoryServerUID getUID();

    /**
     * Gets the network address of this accessory server.
     *
     * @return the server's network address
     */
    InetAddress getAddress();

    /**
     * Gets the network port this accessory server is listening on.
     *
     * @return the server's port number
     */
    int getPort();

    /**
     * Checks if the server is using secure communication.
     *
     * @return true if the server is secure, false otherwise
     */
    boolean isSecure();

    // ==================== Accessory Management ====================

    /**
     * Gets all accessories registered with this server.
     *
     * @return collection of registered accessories
     */
    Collection<Accessory> getAccessories() throws HomekitAccessoryOperationException;

    /**
     * Updates the list of accessories by fetching remote accessories and comparing with currently managed ones.
     * New accessories are added, removed ones are removed, and listeners are notified of changes.
     *
     * @throws IOException if an error occurs while fetching remote accessories
     */
    void updateAccessories() throws HomekitAccessoryOperationException;

    /**
     * Gets an accessory by its accessory ID.
     *
     * @param accessoryId the accessory ID of the accessory
     * @return the accessory, or null if not found
     */
    @Nullable
    Accessory getAccessory(int accessoryId) throws HomekitAccessoryOperationException;

    // /**
    // * Gets an accessory by its class type.
    // *
    // * @param accessoryClass the class type of the accessory
    // * @return the accessory, or null if not found
    // */
    // @Nullable
    // Accessory getAccessory(Class<? extends Accessory> accessoryClass);

    /**
     * Adds a new accessory to this server.
     *
     * @param accessory the accessory to add
     * @throws HomekitAccessoryOperationException if there is an error adding the accessory
     */
    void addAccessory(Accessory accessory) throws HomekitAccessoryOperationException;

    /**
     * Removes an accessory from this server.
     *
     * @param accessory the accessory to remove
     */
    void removeAccessory(Accessory accessory) throws HomekitAccessoryOperationException;

    /**
     * Gets the next available accessory ID.
     * IDs are unique across all accessories in this server.
     *
     * @return the next available accessory ID
     */
    long getNextAvailableAccessoryId() throws HomekitAccessoryOperationException;

    // ==================== Pairing Management ====================

    /**
     * Gets the setup code used for pairing.
     * The code format is ###-##-### and should not be sequential or have repeating patterns.
     *
     * @return the setup code
     */
    String getSetupCode();

    /**
     * Sets the setup code used for pairing.
     *
     * @param setupCode the new setup code
     */
    void setSetupCode(String setupCode);

    /**
     * Gets the server's private key used for encryption.
     *
     * @return the private key
     */
    byte[] getSecretKey();

    /**
     * Gets the server's pairing ID.
     *
     * @return the pairing ID
     */
    byte[] getPairingId();

    /**
     * Adds a new pairing with a client.
     *
     * @param pairingId the client's pairing ID
     * @param publicKey the client's public key
     */
    void addPairing(byte[] pairingId, byte[] publicKey) throws HomekitServerException;

    /**
     * Removes a pairing with a client.
     *
     * @param pairingId the client's pairing ID to remove
     */
    void removePairing(byte[] pairingId) throws HomekitServerException;

    /**
     * Gets a pairing by its ID.
     *
     * @param pairingId the pairing ID
     * @return the pairing, or null if not found
     */
    @Nullable
    Pairing getPairing(byte[] pairingId) throws HomekitServerException;

    /**
     * Gets all active pairings.
     *
     * @return collection of active pairings
     */
    Collection<Pairing> getPairings() throws HomekitServerException;

    /**
     * Gets the public key for a paired client.
     *
     * @param pairingId the client's pairing ID
     * @return the client's public key, or null if not found
     */
    byte @Nullable [] getPublicKey(byte[] pairingId);

    /**
     * Checks if the server has any active pairings.
     *
     * @return true if paired, false otherwise
     */
    default boolean isPaired() {
        return false;
    }

    // ==================== Server Control ====================

    /**
     * Performs a factory reset, removing all pairings and restoring default settings.
     */
    void factoryReset();

    /**
     * Advertises the server on the network.
     */
    void advertise();

    /**
     * Sets the configuration index.
     *
     * @param configurationIndex the new configuration index
     */
    void setConfigurationIndex(int configurationIndex) throws HomekitConfigurationException;

    /**
     * Gets the current configuration index.
     *
     * @return the configuration index
     */
    int getConfigurationIndex();

    // ==================== Pairing Operations ====================

    /**
     * Initiates the pairing setup process.Pair Setup is a one-time operation that creates a valid pairing between an
     * iOS device and an accessory by securely exchanging public keys with an iOS device and an accessory. Pair Setup
     * requires the customer to enter an eight-digit Setup Code on their iOS device. The Setup Code is provided by the
     * accessory via a label or display
     *
     * @throws IOException if an I/O error occurs during setup
     */
    void pairSetup() throws HomekitServerException;

    /**
     * Verifies the pairing with a client. Pair Verify is performed for every HomeKit Accessory Protocol session. Pair
     * Verify verifies the pairing between an iOS device and an accessory and establishes an ephemeral shared secret
     * used to secure the HomeKit Accessory Protocol session
     *
     * @return true if verification successful, false otherwise
     */
    boolean pairVerify() throws HomekitServerException;

    /**
     * Removes the current pairing.
     *
     * @throws HomekitException if an error occurs during removal
     * @throws IOException if an I/O error occurs
     */
    void pairRemove() throws HomekitServerException;

    void start() throws HomekitServerException;

    void stop() throws HomekitServerException;
}
