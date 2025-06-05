package org.openhab.io.homekit.server;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.accessory.HomekitAccessoryCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a persisted HomeKit accessory server configuration.
 *
 * <p>
 * This class provides a data structure for storing and retrieving HomeKit
 * server
 * configurations, including network settings, security keys, and accessory
 * information.
 * It handles the serialization and deserialization of server state for
 * persistence.
 *
 * <p>
 * The class integrates with:
 * <ul>
 * <li>{@link InetAddress} for network address handling</li>
 * <li>{@link HomekitAccessoryCategory} for accessory categorization</li>
 * <li>{@link Base64} for encoding/decoding binary data</li>
 * </ul>
 *
 * <p>
 * Key features:
 * <ul>
 * <li>Network configuration persistence</li>
 * <li>Security key management</li>
 * <li>Accessory list serialization</li>
 * <li>Server type differentiation</li>
 * </ul>
 *
 * @author Karel Goderis - Initial Contribution
 * @since 1.0
 */
@NonNullByDefault
public class HomekitPersistedAccessoryServer {

    // ========== Log Message Prefixes ==========
    private static final Logger logger = LoggerFactory.getLogger(HomekitPersistedAccessoryServer.class);
    private static final String LOG_PREFIX = "Homekit PersistedServer: ";
    private static final String LOG_INIT = LOG_PREFIX + "Init - ";
    private static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    private static final String LOG_ERROR = LOG_PREFIX + "Error - ";

    /**
     * Defines the type of HomeKit server.
     */
    public enum ServerType {
        /** Local server running on the same network */
        LOCAL,
        /** Remote server accessible over the internet */
        REMOTE
    }

    private String localAddress;
    private String port;
    private String pairingIdentifier;
    private String privateKey;
    private String configurationIndex;
    private String accessories;
    private HomekitAccessoryCategory category;
    private ServerType serverType;

    /**
     * Creates a new persisted server with default values.
     */
    public HomekitPersistedAccessoryServer() {
        logger.debug("{}Creating new persisted server with default values", LOG_INIT);
        localAddress = "";
        port = "";
        pairingIdentifier = "";
        privateKey = "";
        configurationIndex = "";
        accessories = "";
        category = HomekitAccessoryCategory.OTHER;
        serverType = ServerType.LOCAL;
    }

    /**
     * Creates a new persisted server with the specified configuration.
     *
     * <p>
     * This constructor initializes all server properties and handles the
     * serialization
     * of binary data and accessory information.
     *
     * @param localAddress The network address to bind to
     * @param port The port to listen on
     * @param pairingId The unique identifier for this server
     * @param privateKey The private key for secure communication
     * @param configurationIndex The current configuration index
     * @param accessories The collection of accessories to persist
     * @param category The category of accessories this server hosts
     * @param serverType The type of server (local or remote)
     */
    public HomekitPersistedAccessoryServer(InetAddress localAddress, int port, byte[] pairingId, byte[] privateKey,
            int configurationIndex, Collection<org.openhab.io.homekit.api.accessory.HomekitAccessory> accessories,
            HomekitAccessoryCategory category, ServerType serverType) {
        logger.debug("{}Creating new persisted server - Address: {}, Port: {}, Category: {}", LOG_INIT, localAddress,
                port, category);
        this.localAddress = localAddress.getHostAddress();
        this.port = Integer.toString(port);
        this.pairingIdentifier = Base64.getEncoder().encodeToString(pairingId);
        this.privateKey = Base64.getEncoder().encodeToString(privateKey);
        this.configurationIndex = Integer.toString(configurationIndex);
        this.category = category;
        this.serverType = serverType;

        // Handle accessories safely
        if (accessories.isEmpty()) {
            this.accessories = "";
        } else {
            this.accessories = accessories.stream().map(Object::toString)
                    .collect(java.util.stream.Collectors.joining(";"));
        }
        logger.debug("{}Persisted server created with {} accessories", LOG_INIT, accessories.size());
    }

    /**
     * Gets the network address of the server.
     *
     * @return The server's network address, or loopback address if resolution fails
     */
    public InetAddress getLocalAddress() {
        try {
            return InetAddress.getByName(localAddress);
        } catch (UnknownHostException e) {
            logger.error("{}Failed to resolve address {}: {}", LOG_ERROR, localAddress, e.getMessage());
            return InetAddress.getLoopbackAddress();
        }
    }

    /**
     * Gets the port number the server listens on.
     *
     * @return The server's port number
     */
    public int getPort() {
        return Integer.parseInt(port);
    }

    /**
     * Gets the pairing identifier for this server.
     *
     * @return The server's pairing identifier
     */
    public byte[] getPairingIdentifier() {
        return Base64.getDecoder().decode(pairingIdentifier);
    }

    /**
     * Gets the private key for this server.
     *
     * @return The server's private key
     */
    public byte[] getPrivateKey() {
        return Base64.getDecoder().decode(privateKey);
    }

    /**
     * Gets the current configuration index.
     *
     * @return The server's configuration index
     */
    public int getConfigurationIndex() {
        return Integer.parseInt(configurationIndex);
    }

    /**
     * Sets the network address of the server.
     *
     * @param localAddress The new network address
     */
    public void setLocalAddress(InetAddress localAddress) {
        logger.debug("{}Setting local address to {}", LOG_CONFIG, localAddress);
        this.localAddress = localAddress.toString();
    }

    /**
     * Sets the port number the server listens on.
     *
     * @param port The new port number
     */
    public void setPort(int port) {
        logger.debug("{}Setting port to {}", LOG_CONFIG, port);
        this.port = Integer.toString(port);
    }

    /**
     * Sets the pairing identifier for this server.
     *
     * @param pairingIdentifier The new pairing identifier
     */
    public void setPairingIdentifier(String pairingIdentifier) {
        logger.debug("{}Setting pairing identifier", LOG_CONFIG);
        this.pairingIdentifier = pairingIdentifier;
    }

    /**
     * Sets the private key for this server.
     *
     * @param privateKey The new private key
     */
    public void setPrivateKey(byte[] privateKey) {
        logger.debug("{}Setting private key", LOG_CONFIG);
        this.privateKey = Base64.getEncoder().encodeToString(privateKey);
    }

    /**
     * Sets the configuration index for this server.
     *
     * @param configurationIndex The new configuration index
     */
    public void setConfigurationIndex(int configurationIndex) {
        logger.debug("{}Setting configuration index to {}", LOG_CONFIG, configurationIndex);
        this.configurationIndex = Integer.toString(configurationIndex);
    }

    /**
     * Sets the list of accessories for this server.
     *
     * @param accessories The collection of accessories to persist
     */
    public void setAccessories(Collection<org.openhab.io.homekit.api.accessory.HomekitAccessory> accessories) {
        logger.debug("{}Setting {} accessories", LOG_CONFIG, accessories.size());
        if (accessories.isEmpty()) {
            this.accessories = "";
        } else {
            this.accessories = accessories.stream().map(accessory -> accessory.getUID().toString())
                    .collect(java.util.stream.Collectors.joining(";"));
        }
    }

    /**
     * Gets the list of accessory UIDs for this server.
     *
     * @return A collection of accessory UIDs
     */
    public Collection<String> getAccessoryUIDs() {
        if (accessories == null || accessories.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(accessories.split(";"));
    }

    /**
     * Gets the category of accessories this server hosts.
     *
     * @return The server's accessory category
     */
    public HomekitAccessoryCategory getCategory() {
        return category;
    }

    /**
     * Sets the category of accessories this server hosts.
     *
     * @param category The new accessory category
     */
    public void setCategory(HomekitAccessoryCategory category) {
        logger.debug("{}Setting category to {}", LOG_CONFIG, category);
        this.category = category;
    }

    /**
     * Gets the type of this server.
     *
     * @return The server's type (local or remote)
     */
    public ServerType getServerType() {
        return serverType;
    }

    /**
     * Sets the type of this server.
     *
     * @param serverType The new server type
     */
    public void setServerType(ServerType serverType) {
        logger.debug("{}Setting server type to {}", LOG_CONFIG, serverType);
        this.serverType = serverType;
    }
}
