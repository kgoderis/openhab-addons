package org.openhab.io.homekit.core.server;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.uid.HomekitAccessoryServerUID;
import org.openhab.io.homekit.util.HomekitUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a unique identifier for a HomeKit accessory server.
 *
 * <p>
 * This class provides a structured way to identify HomeKit accessory servers
 * within the system.
 * The UID follows a specific format: {@code homekit:server:{pairingId}} where:
 * </p>
 * <ul>
 * <li>{@code homekit} is the namespace prefix</li>
 * <li>{@code server} indicates this is a server identifier</li>
 * <li>{@code pairingId} is the unique pairing identifier for the server</li>
 * </ul>
 *
 * <p>
 * Key responsibilities:
 * </p>
 * <ul>
 * <li>Creating and parsing server UIDs</li>
 * <li>Validating UID format and structure</li>
 * <li>Extracting server-specific information from UIDs</li>
 * <li>Ensuring unique identification across the system</li>
 * </ul>
 *
 * <p>
 * The class integrates with:
 * </p>
 * <ul>
 * <li>{@link org.openhab.core.common.registry.Identifiable} for UID
 * management</li>
 * <li>{@link org.openhab.io.homekit.api.server.HomekitAccessoryServer} for
 * server identification</li>
 * <li>OpenHAB's UID system for consistent identification</li>
 * </ul>
 *
 * @author Karel Goderis - Initial contribution
 * @version 1.0
 * @since 1.0
 */
@NonNullByDefault
public class HomekitAccessoryServerUIDImpl extends HomekitUID implements HomekitAccessoryServerUID {
    // ========== Log Message Prefixes ==========
    protected static final String LOG_PREFIX = "Homekit ServerUID: ";
    protected static final String LOG_UID = LOG_PREFIX + "UID - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";

    private final Logger logger = LoggerFactory.getLogger(HomekitAccessoryServerUIDImpl.class);
    private static final String SERVER_PREFIX = "server";
    private String pairingId;

    /**
     * Creates a new server UID with default values.
     *
     * <p>
     * This constructor is package-private and intended for use by reflection only.
     * Not meant for normal instantiation.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Sets up basic UID structure</li>
     * <li>Initializes with empty values</li>
     * <li>Used internally by reflection</li>
     * <li>Provides trace-level logging</li>
     * </ul>
     */
    HomekitAccessoryServerUIDImpl() {
        super(SERVER_PREFIX, "homekit:" + SERVER_PREFIX + ":");
        logger.trace("{}Created default server UID instance", LOG_UID);
    }

    /**
     * Creates a new server UID with the specified pairing ID.
     *
     * <p>
     * This constructor builds a complete server UID instance with the required
     * pairing identifier. The UID is used to uniquely identify a HomeKit
     * accessory server within the system.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Validates all input parameters</li>
     * <li>Constructs the UID string in the correct format</li>
     * <li>Initializes all internal fields</li>
     * <li>Sets up the base UID structure</li>
     * <li>Provides trace-level logging</li>
     * </ul>
     *
     * @param pairingId The unique pairing identifier for the server
     * @throws IllegalArgumentException if the pairing ID is null or empty
     */
    public HomekitAccessoryServerUIDImpl(String pairingId) {
        super(SERVER_PREFIX, "homekit:" + SERVER_PREFIX + ":" + pairingId);
        if (pairingId == null || pairingId.isEmpty()) {
            logger.error("{}Pairing ID cannot be null or empty", LOG_ERROR);
            throw new IllegalArgumentException("Pairing ID cannot be null or empty");
        }
        this.pairingId = pairingId;
        logger.trace("{}Created server UID with pairing ID: {}", LOG_UID, pairingId);
    }

    /**
     * Gets the UID as a string.
     *
     * <p>
     * The string representation follows the format
     * {@code homekit:server:{pairingId}}.
     * This format ensures consistent identification across the system.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Uses String.format for consistent formatting</li>
     * <li>Maintains the standard UID structure</li>
     * <li>Preserves all identifier components</li>
     * <li>Provides trace-level logging</li>
     * </ul>
     *
     * @return The UID string in the format {@code homekit:server:{pairingId}}
     */
    @Override
    public String toString() {
        String result = String.format("homekit:server:%s", pairingId);
        logger.trace("{}Getting UID string: {}", LOG_UID, result);
        return result;
    }

    /**
     * Gets the pairing ID for this server.
     *
     * <p>
     * The pairing ID is a unique identifier used during the HomeKit pairing
     * process.
     * It helps maintain the connection between the server and its paired devices.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Returns the internal pairing ID field</li>
     * <li>Used for server connection management</li>
     * <li>Supports device pairing</li>
     * <li>Provides trace-level logging</li>
     * </ul>
     *
     * @return The pairing identifier for this server
     */
    @Override
    public String getPairingId() {
        logger.trace("{}Getting pairing ID: {}", LOG_UID, pairingId);
        return pairingId;
    }

    /**
     * Gets the minimum number of segments required for a valid UID.
     *
     * <p>
     * A valid server UID must have at least 3 segments:
     * </p>
     * <ol>
     * <li>The namespace prefix ("homekit")</li>
     * <li>The type identifier ("server")</li>
     * <li>The pairing ID</li>
     * </ol>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Enforces UID structure validation</li>
     * <li>Ensures complete identification</li>
     * <li>Supports UID parsing</li>
     * <li>Provides trace-level logging</li>
     * </ul>
     *
     * @return The minimum number of segments (3) for a valid server UID
     */
    @Override
    protected int getMinimalNumberOfSegments() {
        logger.trace("{}Getting minimal number of segments: 3", LOG_UID);
        return 3;
    }

    /**
     * Gets this UID instance.
     *
     * <p>
     * This method provides access to the UID instance itself, maintaining
     * consistency with the {@link HomekitAccessoryServerUID} interface.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Returns this instance</li>
     * <li>Supports interface compliance</li>
     * <li>Enables UID access</li>
     * <li>Provides trace-level logging</li>
     * </ul>
     *
     * @return This UID instance
     */
    @Override
    public HomekitAccessoryServerUID getUID() {
        logger.trace("{}Getting UID instance", LOG_UID);
        return this;
    }
}
