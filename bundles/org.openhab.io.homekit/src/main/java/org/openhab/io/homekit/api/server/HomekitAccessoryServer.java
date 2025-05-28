package org.openhab.io.homekit.api.server;

import java.net.InetAddress;
import java.util.Collection;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.Identifiable;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.uid.HomekitAccessoryServerUID;
import org.openhab.io.homekit.exception.HomekitAccessoryOperationException;
import org.openhab.io.homekit.exception.HomekitConfigurationException;
import org.openhab.io.homekit.exception.HomekitServerException;
import org.openhab.io.homekit.protocol.pairing.HomekitPairing;

/**
 * Interface for an HAP HomeKit Accessory Server that exposes a collection of accessories to HomeKit controllers.
 * <p>
 * This interface defines the core functionality for a HomeKit accessory server, which serves as one endpoint
 * of the pairing relationship established with HomeKit controllers. The server manages accessories, handles
 * pairing operations, and controls server behavior.
 * </p>
 * <p>
 * The server provides:
 * <ul>
 * <li>Accessory registration and management</li>
 * <li>Secure pairing and authentication</li>
 * <li>Network discovery and advertising</li>
 * <li>Configuration management</li>
 * <li>Server lifecycle control</li>
 * </ul>
 * </p>
 * <p>
 * Key implementation details:
 * <ul>
 * <li>Secure communication using encryption</li>
 * <li>Unique accessory identification</li>
 * <li>Pairing state management</li>
 * <li>Network service discovery</li>
 * <li>Configuration versioning</li>
 * </ul>
 * </p>
 * <p>
 * The interface integrates with:
 * <ul>
 * <li>{@link org.openhab.io.homekit.api.accessory.HomekitAccessory} for accessory management</li>
 * <li>{@link org.openhab.io.homekit.protocol.pairing.HomekitPairing} for pairing operations</li>
 * <li>{@link org.openhab.io.homekit.api.uid.HomekitAccessoryServerUID} for server identification</li>
 * <li>{@link org.openhab.core.common.registry.Identifiable} for registry integration</li>
 * <li>{@link org.openhab.io.homekit.exception.HomekitAccessoryOperationException} for accessory errors</li>
 * <li>{@link org.openhab.io.homekit.exception.HomekitConfigurationException} for configuration errors</li>
 * <li>{@link org.openhab.io.homekit.exception.HomekitServerException} for server errors</li>
 * </ul>
 * </p>
 *
 * @author Karel Goderis - Initial Contribution
 * @since 1.0.0
 */
@NonNullByDefault
public interface HomekitAccessoryServer extends Identifiable<HomekitAccessoryServerUID> {

    // ==================== Core Server Methods ====================

    /**
     * Gets the unique identifier for this accessory server.
     * This ID is used for advertising and pairing purposes.
     *
     * @return the unique server identifier
     * @since 1.0.0
     */
    @Override
    HomekitAccessoryServerUID getUID();

    /**
     * Gets the network address of this accessory server.
     * This address is used for network communication and discovery.
     *
     * @return the server's network address
     * @since 1.0.0
     */
    InetAddress getAddress();

    /**
     * Gets the network port this accessory server is listening on.
     * The port is used for incoming connections from HomeKit controllers.
     *
     * @return the server's port number
     * @since 1.0.0
     */
    int getPort();

    /**
     * Checks if the server is using secure communication.
     * Secure communication is required for HomeKit compatibility.
     *
     * @return true if the server is secure, false otherwise
     * @since 1.0.0
     */
    boolean isSecure();

    // ==================== HomekitAccessory Management ====================

    /**
     * Gets all accessories registered with this server.
     * This method retrieves the complete collection of accessories managed by the server.
     *
     * @return collection of registered accessories
     * @throws HomekitAccessoryOperationException if there is an error retrieving the accessories
     * @since 1.0.0
     */
    Collection<HomekitAccessory> getAccessories() throws HomekitAccessoryOperationException;

    /**
     * Updates the list of accessories by fetching remote accessories and comparing with currently managed ones.
     * This method:
     * <ul>
     * <li>Fetches remote accessories</li>
     * <li>Compares with current accessories</li>
     * <li>Adds new accessories</li>
     * <li>Removes obsolete accessories</li>
     * <li>Notifies listeners of changes</li>
     * </ul>
     *
     * @throws HomekitAccessoryOperationException if there is an error updating the accessories
     * @since 1.0.0
     */
    void updateAccessories() throws HomekitAccessoryOperationException;

    /**
     * Gets an accessory by its accessory ID.
     * The accessory ID is a unique identifier within the server.
     *
     * @param accessoryId the accessory ID of the accessory
     * @return the accessory, or null if not found
     * @throws HomekitAccessoryOperationException if there is an error retrieving the accessory
     * @since 1.0.0
     */
    @Nullable
    HomekitAccessory getAccessory(int accessoryId) throws HomekitAccessoryOperationException;

    /**
     * Adds a new accessory to this server.
     * The accessory must have a unique ID within the server.
     *
     * @param accessory the accessory to add
     * @throws HomekitAccessoryOperationException if there is an error adding the accessory
     * @since 1.0.0
     */
    void addAccessory(HomekitAccessory accessory) throws HomekitAccessoryOperationException;

    /**
     * Removes an accessory from this server.
     * This operation also cleans up any associated resources.
     *
     * @param accessory the accessory to remove
     * @throws HomekitAccessoryOperationException if there is an error removing the accessory
     * @since 1.0.0
     */
    void removeAccessory(HomekitAccessory accessory) throws HomekitAccessoryOperationException;

    /**
     * Gets the next available accessory ID.
     * IDs are unique across all accessories in this server.
     *
     * @return the next available accessory ID
     * @throws HomekitAccessoryOperationException if there is an error generating the ID
     * @since 1.0.0
     */
    long getNextAvailableAccessoryId() throws HomekitAccessoryOperationException;

    // ==================== HomekitPairing Management ====================

    /**
     * Gets the setup code used for pairing.
     * The code format is ###-##-### and should not be sequential or have repeating patterns.
     *
     * @return the setup code
     * @since 1.0.0
     */
    String getSetupCode();

    /**
     * Sets the setup code used for pairing.
     * The code must follow the required format and security guidelines.
     *
     * @param setupCode the new setup code
     * @since 1.0.0
     */
    void setSetupCode(String setupCode);

    /**
     * Gets the server's private key used for encryption.
     * This key is used for secure communication with HomeKit controllers.
     *
     * @return the private key
     * @since 1.0.0
     */
    byte[] getSecretKey();

    /**
     * Gets the server's pairing ID.
     * This ID is used to identify the server during pairing operations.
     *
     * @return the pairing ID
     * @since 1.0.0
     */
    byte[] getPairingId();

    /**
     * Adds a new pairing with a client.
     * This method establishes a secure connection with a HomeKit controller.
     *
     * @param pairingId the client's pairing ID
     * @param publicKey the client's public key
     * @throws HomekitServerException if there is an error adding the pairing
     * @since 1.0.0
     */
    void addPairing(byte[] pairingId, byte[] publicKey) throws HomekitServerException;

    /**
     * Removes a pairing with a client.
     * This method terminates the secure connection with a HomeKit controller.
     *
     * @param pairingId the client's pairing ID to remove
     * @throws HomekitServerException if there is an error removing the pairing
     * @since 1.0.0
     */
    void removePairing(byte[] pairingId) throws HomekitServerException;

    /**
     * Gets a pairing by its ID.
     * This method retrieves the pairing information for a specific client.
     *
     * @param pairingId the pairing ID
     * @return the pairing, or null if not found
     * @throws HomekitServerException if there is an error retrieving the pairing
     * @since 1.0.0
     */
    @Nullable
    HomekitPairing getPairing(byte[] pairingId) throws HomekitServerException;

    /**
     * Gets all active pairings.
     * This method retrieves all current pairings with HomeKit controllers.
     *
     * @return collection of active pairings
     * @throws HomekitServerException if there is an error retrieving the pairings
     * @since 1.0.0
     */
    Collection<HomekitPairing> getPairings() throws HomekitServerException;

    /**
     * Gets the public key for a paired client.
     * This key is used for secure communication with the client.
     *
     * @param pairingId the client's pairing ID
     * @return the client's public key, or null if not found
     * @since 1.0.0
     */
    byte @Nullable [] getPublicKey(byte[] pairingId);

    /**
     * Checks if the server has any active pairings.
     * This method indicates whether the server is currently paired with any HomeKit controllers.
     *
     * @return true if paired, false otherwise
     * @since 1.0.0
     */
    default boolean isPaired() {
        return false;
    }

    // ==================== Server Control ====================

    /**
     * Performs a factory reset, removing all pairings and restoring default settings.
     * This method:
     * <ul>
     * <li>Removes all pairings</li>
     * <li>Resets configuration</li>
     * <li>Clears accessory state</li>
     * <li>Restores default settings</li>
     * </ul>
     *
     * @since 1.0.0
     */
    void factoryReset();

    /**
     * Advertises the server on the network.
     * This method makes the server discoverable by HomeKit controllers.
     *
     * @since 1.0.0
     */
    void advertise();

    /**
     * Sets the configuration index.
     * The configuration index is used to track changes in the server's configuration.
     *
     * @param configurationIndex the new configuration index
     * @throws HomekitConfigurationException if there is an error setting the configuration
     * @since 1.0.0
     */
    void setConfigurationIndex(int configurationIndex) throws HomekitConfigurationException;

    /**
     * Gets the current configuration index.
     * This index is used to track changes in the server's configuration.
     *
     * @return the configuration index
     * @since 1.0.0
     */
    int getConfigurationIndex();

    // ==================== HomekitPairing Operations ====================

    /**
     * Initiates the pairing setup process.
     * <p>
     * Pair Setup is a one-time operation that creates a valid pairing between an
     * iOS device and an accessory by securely exchanging public keys. The process requires
     * the customer to enter an eight-digit Setup Code on their iOS device.
     * </p>
     *
     * @throws HomekitServerException if there is an error during setup
     * @since 1.0.0
     */
    void pairSetup() throws HomekitServerException;

    /**
     * Verifies the pairing with a client.
     * <p>
     * Pair Verify is performed for every HomeKit Accessory Protocol session to:
     * <ul>
     * <li>Verify the pairing between an iOS device and an accessory</li>
     * <li>Establish an ephemeral shared secret</li>
     * <li>Secure the HomeKit Accessory Protocol session</li>
     * </ul>
     * </p>
     *
     * @return true if verification successful, false otherwise
     * @throws HomekitServerException if there is an error during verification
     * @since 1.0.0
     */
    boolean pairVerify() throws HomekitServerException;

    /**
     * Removes the current pairing.
     * This method terminates the secure connection with the current HomeKit controller.
     *
     * @throws HomekitServerException if there is an error removing the pairing
     * @since 1.0.0
     */
    void pairRemove() throws HomekitServerException;

    /**
     * Starts the server.
     * This method initializes the server and begins accepting connections.
     *
     * @throws HomekitServerException if there is an error starting the server
     * @since 1.0.0
     */
    void start() throws HomekitServerException;

    /**
     * Stops the server.
     * This method shuts down the server and releases resources.
     *
     * @throws HomekitServerException if there is an error stopping the server
     * @since 1.0.0
     */
    void stop() throws HomekitServerException;
}
