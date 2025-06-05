package org.openhab.io.homekit.core.characteristic;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.uid.HomekitCharacteristicUID;
import org.openhab.io.homekit.util.HomekitUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a unique identifier for a HomeKit characteristic.
 *
 * <p>
 * This class provides a structured way to identify HomeKit characteristics
 * within the system.
 * The UID follows a specific format:
 * {@code homekit:characteristic:{pairingId}:{accessoryId}:{serviceId}:{characteristicId}}
 * where:
 * </p>
 * <ul>
 * <li>{@code homekit} is the namespace prefix</li>
 * <li>{@code characteristic} indicates this is a characteristic identifier</li>
 * <li>{@code pairingId} is the unique pairing identifier for the server</li>
 * <li>{@code accessoryId} is the unique identifier for the accessory</li>
 * <li>{@code serviceId} is the unique identifier for the service</li>
 * <li>{@code characteristicId} is the unique identifier for the
 * characteristic</li>
 * </ul>
 *
 * <p>
 * Key responsibilities:
 * </p>
 * <ul>
 * <li>Creating and parsing characteristic UIDs</li>
 * <li>Validating UID format and structure</li>
 * <li>Extracting characteristic-specific information from UIDs</li>
 * <li>Ensuring unique identification across the system</li>
 * </ul>
 *
 * <p>
 * The class integrates with:
 * </p>
 * <ul>
 * <li>{@link org.openhab.core.common.registry.Identifiable} for UID
 * management</li>
 * <li>{@link org.openhab.io.homekit.api.characteristic.HomekitCharacteristic}
 * for characteristic identification</li>
 * <li>OpenHAB's UID system for consistent identification</li>
 * </ul>
 *
 * @author Karel Goderis - Initial contribution
 * @version 1.0
 * @since 1.0
 */
@NonNullByDefault
public class HomekitCharacteristicUIDImpl extends HomekitUID implements HomekitCharacteristicUID {
    // ========== Log Message Prefixes ==========
    protected static final String LOG_PREFIX = "Homekit CharacteristicUID: ";
    protected static final String LOG_UID = LOG_PREFIX + "UID - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";

    private final Logger logger = LoggerFactory.getLogger(HomekitCharacteristicUIDImpl.class);
    private static final String CHARACTERISTIC_PREFIX = "characteristic";
    private final long instanceId;
    private final String pairingId;
    private final long accessoryId;
    private final long serviceId;
    private final long characteristicId;

    /**
     * Creates a new characteristic UID with the specified components.
     *
     * <p>
     * This constructor builds a complete characteristic UID instance with all
     * required
     * identifiers. The UID is used to uniquely identify a HomeKit characteristic
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
     * <li>Provides trace-level logging</li>
     * </ul>
     *
     * @param pairingId The unique pairing identifier for the server
     * @param accessoryId The unique identifier for the accessory
     * @param serviceId The unique identifier for the service
     * @param characteristicId The unique identifier for the characteristic
     * @throws IllegalArgumentException if any of the IDs are null or empty
     */
    public HomekitCharacteristicUIDImpl(String pairingId, long accessoryId, long serviceId, long characteristicId) {
        super(CHARACTERISTIC_PREFIX, "homekit:" + CHARACTERISTIC_PREFIX + ":" + pairingId + ":" + accessoryId + ":"
                + serviceId + ":" + characteristicId);
        if (pairingId == null || pairingId.isEmpty()) {
            logger.error("{}Pairing ID cannot be null or empty", LOG_ERROR);
            throw new IllegalArgumentException("Pairing ID cannot be null or empty");
        }
        this.pairingId = pairingId;
        this.accessoryId = accessoryId;
        this.serviceId = serviceId;
        this.characteristicId = characteristicId;
        this.instanceId = 0;
        logger.trace(
                "{}Created characteristic UID with pairing ID: {}, accessory ID: {}, service ID: {}, characteristic ID: {}",
                LOG_UID, pairingId, accessoryId, serviceId, characteristicId);
    }

    /**
     * Creates a new characteristic UID from a string key.
     *
     * <p>
     * This constructor parses an existing UID string into a characteristic
     * identifier.
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
     * <li>Provides trace-level logging</li>
     * </ul>
     *
     * @param key The string representation of the UID
     * @throws IllegalArgumentException if the key format is invalid
     */
    public HomekitCharacteristicUIDImpl(String key) {
        super("characteristic", key);
        List<String> segments = getAllSegments();
        if (segments.size() < getMinimalNumberOfSegments()) {
            logger.error("{}Invalid characteristic UID format: {}", LOG_ERROR, key);
            throw new IllegalArgumentException("Invalid characteristic UID format: " + key);
        }
        this.pairingId = segments.get(2);
        this.accessoryId = Long.parseLong(segments.get(3));
        this.serviceId = Long.parseLong(segments.get(4));
        this.characteristicId = Long.parseLong(segments.get(5));
        this.instanceId = 0;
        logger.trace(
                "{}Parsed characteristic UID from key: {} with pairing ID: {}, accessory ID: {}, service ID: {}, characteristic ID: {}",
                LOG_UID, key, pairingId, accessoryId, serviceId, characteristicId);
    }

    /**
     * Gets the UID as a string.
     *
     * <p>
     * The string representation follows the format
     * {@code homekit:characteristic:{pairingId}:{accessoryId}:{serviceId}:{characteristicId}}.
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
     * @return The UID string in the format
     *         {@code homekit:characteristic:{pairingId}:{accessoryId}:{serviceId}:{characteristicId}}
     */
    @Override
    public String toString() {
        String result = String.format("homekit:characteristic:%s:%s:%s:%s", pairingId, accessoryId, serviceId,
                characteristicId);
        logger.trace("{}Getting UID string: {}", LOG_UID, result);
        return result;
    }

    /**
     * Gets the instance ID of this characteristic.
     *
     * <p>
     * The instance ID is a unique identifier used to distinguish between
     * multiple characteristics of the same type within a service.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Returns the internal instance ID field</li>
     * <li>Used for characteristic differentiation</li>
     * <li>Supports multiple instances of the same type</li>
     * <li>Provides trace-level logging</li>
     * </ul>
     *
     * @return The characteristic instance ID
     */
    @Override
    public long getInstanceId() {
        logger.trace("{}Getting instance ID: {}", LOG_UID, instanceId);
        return instanceId;
    }

    /**
     * Gets the minimum number of segments required for a valid UID.
     *
     * <p>
     * A valid characteristic UID must have at least 6 segments:
     * </p>
     * <ol>
     * <li>The namespace prefix ("homekit")</li>
     * <li>The type identifier ("characteristic")</li>
     * <li>The pairing ID</li>
     * <li>The accessory ID</li>
     * <li>The service ID</li>
     * <li>The characteristic ID</li>
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
     * @return The minimum number of segments (6) for a valid characteristic UID
     */
    @Override
    protected int getMinimalNumberOfSegments() {
        logger.trace("{}Getting minimal number of segments: 6", LOG_UID);
        return 6;
    }

    /**
     * Gets this UID instance.
     *
     * <p>
     * This method provides access to the UID instance itself, maintaining
     * consistency with the {@link HomekitCharacteristicUID} interface.
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
    public HomekitCharacteristicUID getUID() {
        logger.trace("{}Getting UID instance", LOG_UID);
        return this;
    }

    /**
     * Returns the HomeKit ID part of this characteristic UID.
     *
     * <p>
     * This method extracts the last 4 segments of the UID and joins them with the
     * separator
     * to form the HomeKit-specific identifier. The resulting string contains only
     * the
     * essential identification components without the namespace and type prefixes.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Extracts the last 4 segments</li>
     * <li>Joins segments with the standard separator</li>
     * <li>Maintains identifier order</li>
     * <li>Provides trace-level logging</li>
     * </ul>
     *
     * @return The HomeKit ID in the format:
     *         pairingId:accessoryId:serviceId:characteristicId
     */
    public String getHomekitId() {
        String result = String.join(SEPARATOR,
                getAllSegments().subList(getAllSegments().size() - 4, getAllSegments().size()));
        logger.trace("{}Getting HomeKit ID: {}", LOG_UID, result);
        return result;
    }

    /**
     * Gets all segments of this UID.
     *
     * <p>
     * This method provides access to the individual components of the UID,
     * allowing for detailed inspection and manipulation of the identifier parts.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Returns all UID segments</li>
     * <li>Maintains segment order</li>
     * <li>Supports UID analysis</li>
     * <li>Provides trace-level logging</li>
     * </ul>
     *
     * @return A list of all UID segments
     */
    @Override
    protected List<String> getAllSegments() {
        List<String> segments = super.getAllSegments();
        logger.trace("{}Getting all segments: {}", LOG_UID, segments);
        return segments;
    }
}
