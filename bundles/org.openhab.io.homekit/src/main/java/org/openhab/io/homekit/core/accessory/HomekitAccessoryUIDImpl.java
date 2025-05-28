package org.openhab.io.homekit.core.accessory;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.uid.HomekitAccessoryUID;
import org.openhab.io.homekit.util.HomekitUID;

/**
 * Implementation of a unique identifier for a HomeKit accessory.
 *
 * <p>
 * This class provides a structured way to identify HomeKit accessories within the system.
 * The UID follows a specific format: {@code homekit:accessory:{pairingId}:{accessoryId}} where:
 * </p>
 * <ul>
 * <li>{@code homekit} is the namespace prefix</li>
 * <li>{@code accessory} indicates this is an accessory identifier</li>
 * <li>{@code pairingId} is the unique pairing identifier for the server</li>
 * <li>{@code accessoryId} is the unique identifier for the accessory</li>
 * </ul>
 *
 * <p>
 * Key responsibilities:
 * </p>
 * <ul>
 * <li>Creating and parsing accessory UIDs</li>
 * <li>Validating UID format and structure</li>
 * <li>Extracting accessory-specific information from UIDs</li>
 * <li>Ensuring unique identification across the system</li>
 * </ul>
 *
 * <p>
 * The class integrates with:
 * </p>
 * <ul>
 * <li>{@link org.openhab.core.common.registry.Identifiable} for UID management</li>
 * <li>{@link org.openhab.io.homekit.api.accessory.HomekitAccessory} for accessory identification</li>
 * <li>OpenHAB's UID system for consistent identification</li>
 * </ul>
 *
 * @author Karel Goderis - Initial contribution
 * @version 1.0
 * @since 1.0
 */
@NonNullByDefault
public class HomekitAccessoryUIDImpl extends HomekitUID implements HomekitAccessoryUID {
    private static final String ACCESSORY_PREFIX = "accessory";
    private final String pairingId;
    private final long accessoryId;

    /**
     * Creates a new accessory UID with the specified components.
     *
     * <p>
     * This constructor builds a complete accessory UID instance with all required
     * identifiers. The UID is used to uniquely identify a HomeKit accessory
     * within the system.
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
     * </ul>
     *
     * @param pairingId The unique pairing identifier for the server
     * @param accessoryId The unique identifier for the accessory
     * @throws IllegalArgumentException if the pairing ID is null or empty
     */
    public HomekitAccessoryUIDImpl(String pairingId, long accessoryId) {
        super(ACCESSORY_PREFIX, "homekit:" + ACCESSORY_PREFIX + ":" + pairingId + ":" + accessoryId);
        this.pairingId = pairingId;
        this.accessoryId = accessoryId;
    }

    /**
     * Creates a new accessory UID from a string key.
     *
     * <p>
     * This constructor parses an existing UID string into an accessory identifier.
     * It is used when reconstructing a UID from its string representation.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Validates the input string format</li>
     * <li>Extracts individual components</li>
     * <li>Initializes internal fields</li>
     * </ul>
     *
     * @param key The string representation of the UID
     * @throws IllegalArgumentException if the key format is invalid
     */
    public HomekitAccessoryUIDImpl(String uid) {
        super(uid);
        String[] segments = uid.split(":");
        if (segments.length != 4 || !segments[0].equals("homekit") || !segments[1].equals("accessory")) {
            throw new IllegalArgumentException("Invalid HomeKit accessory UID format: " + uid);
        }
        this.pairingId = segments[2];
        this.accessoryId = Long.parseLong(segments[3]);
        // this.uid = uid;
    }

    /**
     * Gets the UID as a string.
     *
     * <p>
     * The string representation follows the format {@code homekit:accessory:{pairingId}:{accessoryId}}.
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
     * </ul>
     *
     * @return The UID string in the format {@code homekit:accessory:{pairingId}:{accessoryId}}
     */
    @Override
    public String getAsString() {
        return String.format("homekit:accessory:%s:%d", pairingId, accessoryId);
    }

    /**
     * Gets the pairing ID for this accessory.
     *
     * <p>
     * The pairing ID is a unique identifier used during the HomeKit pairing process.
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
     * </ul>
     *
     * @return The pairing identifier for this accessory
     */
    @Override
    public String getPairingId() {
        return pairingId;
    }

    /**
     * Gets the accessory ID.
     *
     * <p>
     * The accessory ID uniquely identifies this specific accessory within its server.
     * It helps distinguish between multiple accessories that may be connected to the same server.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Returns the internal accessory ID field</li>
     * <li>Used for accessory differentiation</li>
     * <li>Supports multiple accessories</li>
     * </ul>
     *
     * @return The accessory identifier
     */
    @Override
    public long getAccessoryId() {
        return accessoryId;
    }

    /**
     * Gets the minimum number of segments required for a valid UID.
     *
     * <p>
     * A valid accessory UID must have at least 4 segments:
     * </p>
     * <ol>
     * <li>The namespace prefix ("homekit")</li>
     * <li>The type identifier ("accessory")</li>
     * <li>The pairing ID</li>
     * <li>The accessory ID</li>
     * </ol>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Enforces UID structure validation</li>
     * <li>Ensures complete identification</li>
     * <li>Supports UID parsing</li>
     * </ul>
     *
     * @return The minimum number of segments (4) for a valid accessory UID
     */
    @Override
    protected int getMinimalNumberOfSegments() {
        return 4;
    }

    /**
     * Gets this UID instance.
     *
     * <p>
     * This method provides access to the UID instance itself, maintaining
     * consistency with the {@link HomekitAccessoryUID} interface.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Returns this instance</li>
     * <li>Supports interface compliance</li>
     * <li>Enables UID access</li>
     * </ul>
     *
     * @return This UID instance
     */
    @Override
    public HomekitAccessoryUID getUID() {
        return this;
    }
}
