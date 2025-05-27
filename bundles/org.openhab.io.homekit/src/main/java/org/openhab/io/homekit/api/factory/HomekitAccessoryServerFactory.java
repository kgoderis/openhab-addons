package org.openhab.io.homekit.api.factory;

import java.net.InetAddress;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.io.homekit.api.server.HomekitAccessoryServer;

//TODO : ServerFactory that can create AccessoryHolder for the client side
// Rename to AccessoryHolderFactory

/**
 * Factory interface for creating HomeKit accessory servers.
 *
 * This interface defines the contract for factories that create HomeKit accessory servers. It provides
 * methods for creating server instances with different configurations and for querying supported server types.
 *
 * The factory provides:
 * - Server instance creation with basic configuration
 * - Server instance creation with advanced security settings
 * - Server type discovery
 * - Network configuration management
 * - Security parameter handling
 *
 * Key implementation details:
 * - Thread-safe server creation
 * - Network address binding
 * - Port management
 * - Security parameter validation
 * - Server type registration
 *
 * The interface integrates with:
 * - {@link org.openhab.io.homekit.api.server.HomekitAccessoryServer} for server instances
 * - {@link java.net.InetAddress} for network configuration
 *
 * @author Karel Goderis - Initial Contribution
 * @since 1.0.0
 */
@NonNullByDefault
public interface HomekitAccessoryServerFactory {

    /**
     * Creates a new HomeKit accessory server instance with basic configuration.
     * This method creates a server with default security settings.
     *
     * @param factoryType The type of server to create
     * @param localAddress The local network address to bind to
     * @param port The port number to listen on
     * @return A new server instance, or null if the factory type is not supported
     * @since 1.0.0
     */
    @Nullable
    HomekitAccessoryServer createServer(String factoryType, InetAddress localAddress, int port);

    /**
     * Creates a new HomeKit accessory server instance with advanced security configuration.
     * This method creates a server with custom security parameters for pairing and encryption.
     *
     * @param factoryType The type of server to create
     * @param localAddress The local network address to bind to
     * @param port The port number to listen on
     * @param pairingId The unique identifier for pairing
     * @param privateKey The private key for encryption
     * @param configurationIndex The configuration index for the server
     * @return A new server instance, or null if the factory type is not supported
     * @since 1.0.0
     */
    @Nullable
    HomekitAccessoryServer createServer(String factoryType, InetAddress localAddress, int port, byte[] pairingId,
            byte[] privateKey, int configurationIndex);

    /**
     * Gets all server types supported by this factory.
     * This method provides a complete list of server types that can be created.
     *
     * @return An unmodifiable set of supported server type identifiers
     * @since 1.0.0
     */
    Set<String> getSupportedServerTypes();

    // /**
    // * This is used to salt the setup code during pairing.
    // *
    // * @return the generated salt
    // */
    // BigInteger generateSalt();

    // /**
    // * This is used as the private key during pairing and connection setup
    // * setup.
    // *
    // * @return the generated key
    // * @throws InvalidAlgorithmParameterException if the JVM does not contain the necessary encryption
    // * algorithms.
    // */
    // byte[] generatePrivateKey() throws InvalidAlgorithmParameterException;
    //
    // /**
    // * This is used as the unique identifier of the HomekitAccessoryServer during mDNS advertising. It is a valid MAC
    // * address generated in the locally administered range so as not to conflict with any commercial
    // * devices.
    // *
    // * @return the generated pairing id
    // */
    // byte[] generatePairingId();
}
