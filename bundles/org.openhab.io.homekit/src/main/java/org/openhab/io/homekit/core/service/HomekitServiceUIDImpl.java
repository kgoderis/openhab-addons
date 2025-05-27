package org.openhab.io.homekit.core.service;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.uid.HomekitServiceUID;
import org.openhab.io.homekit.util.HomekitUID;

import java.util.List;

/**
 * Implementation of a unique identifier for a HomeKit service.
 *
 * <p>
 * This class provides a structured way to identify HomeKit services within the system.
 * The UID follows a specific format: {@code homekit:service:{pairingId}:{accessoryId}:{serviceId}} where:
 * </p>
 * <ul>
 *   <li>{@code homekit} is the namespace prefix</li>
 *   <li>{@code service} indicates this is a service identifier</li>
 *   <li>{@code pairingId} is the unique pairing identifier for the server</li>
 *   <li>{@code accessoryId} is the unique identifier for the accessory</li>
 *   <li>{@code serviceId} is the unique identifier for the service</li>
 * </ul>
 *
 * <p>
 * Key responsibilities:
 * </p>
 * <ul>
 *   <li>Creating and parsing service UIDs</li>
 *   <li>Validating UID format and structure</li>
 *   <li>Extracting service-specific information from UIDs</li>
 *   <li>Ensuring unique identification across the system</li>
 * </ul>
 *
 * <p>
 * The class integrates with:
 * </p>
 * <ul>
 *   <li>{@link org.openhab.core.common.registry.Identifiable} for UID management</li>
 *   <li>{@link org.openhab.io.homekit.api.service.HomekitService} for service identification</li>
 *   <li>OpenHAB's UID system for consistent identification</li>
 * </ul>
 *
 * @author Karel Goderis - Initial contribution
 * @version 1.0
 * @since 1.0
 */
@NonNullByDefault
public class HomekitServiceUIDImpl extends HomekitUID implements HomekitServiceUID {
    private static final String SERVICE_PREFIX = "service";
    private final long instanceId;
    private final String pairingId;
    private final long accessoryId;
    private final long serviceId;

    /**
     * Creates a new service UID with the specified components.
     *
     * <p>
     * This constructor builds a complete service UID instance with all required
     * identifiers. The UID is used to uniquely identify a HomeKit service
     * within the system.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     *   <li>Validates all input parameters</li>
     *   <li>Constructs the UID string in the correct format</li>
     *   <li>Initializes all internal fields</li>
     *   <li>Sets up the base UID structure</li>
     * </ul>
     *
     * @param pairingId The unique pairing identifier for the server
     * @param accessoryId The unique identifier for the accessory
     * @param serviceId The unique identifier for the service
     * @throws IllegalArgumentException if any of the IDs are null or empty
     */
    public HomekitServiceUIDImpl(String pairingId, long accessoryId, long serviceId) {
        super(SERVICE_PREFIX, "homekit:" + SERVICE_PREFIX + ":" + pairingId + ":" + accessoryId + ":" + serviceId);
        this.pairingId = pairingId;
        this.accessoryId = accessoryId;
        this.serviceId = serviceId;
        this.instanceId = 0;
    }

    /**
     * Creates a new service UID from a string key.
     *
     * <p>
     * This constructor parses an existing UID string into a service identifier.
     * It is used when reconstructing a UID from its string representation.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     *   <li>Validates the input string format</li>
     *   <li>Extracts individual components</li>
     *   <li>Initializes internal fields</li>
     * </ul>
     *
     * @param key The string representation of the UID
     * @throws IllegalArgumentException if the key format is invalid
     */
    public HomekitServiceUIDImpl(String key) {
        super("service", key);
        List<String> segments = getAllSegments();
        if (segments.size() < getMinimalNumberOfSegments()) {
            throw new IllegalArgumentException("Invalid service UID format: " + key);
        }
        this.pairingId = segments.get(2);
        this.accessoryId = Long.parseLong(segments.get(3));
        this.serviceId = Long.parseLong(segments.get(4));
        this.instanceId = 0;
    }

    /**
     * Gets the UID as a string.
     *
     * <p>
     * The string representation follows the format {@code homekit:service:{pairingId}:{accessoryId}:{serviceId}}.
     * This format ensures consistent identification across the system.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     *   <li>Uses String.format for consistent formatting</li>
     *   <li>Maintains the standard UID structure</li>
     *   <li>Preserves all identifier components</li>
     * </ul>
     *
     * @return The UID string in the format {@code homekit:service:{pairingId}:{accessoryId}:{serviceId}}
     */
    @Override
    public String getAsString() {
        return String.format("homekit:service:%s:%s:%s", pairingId, accessoryId, serviceId);
    }

    /**
     * Gets the instance ID of this service.
     *
     * <p>
     * The instance ID is a unique identifier used to distinguish between
     * multiple services of the same type within an accessory.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     *   <li>Returns the internal instance ID field</li>
     *   <li>Used for service differentiation</li>
     *   <li>Supports multiple instances of the same type</li>
     * </ul>
     *
     * @return The service instance ID
     */
    @Override
    public long getInstanceId() {
        return instanceId;
    }

    /**
     * Gets the minimum number of segments required for a valid UID.
     *
     * <p>
     * A valid service UID must have at least 5 segments:
     * </p>
     * <ol>
     *   <li>The namespace prefix ("homekit")</li>
     *   <li>The type identifier ("service")</li>
     *   <li>The pairing ID</li>
     *   <li>The accessory ID</li>
     *   <li>The service ID</li>
     * </ol>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     *   <li>Enforces UID structure validation</li>
     *   <li>Ensures complete identification</li>
     *   <li>Supports UID parsing</li>
     * </ul>
     *
     * @return The minimum number of segments (5) for a valid service UID
     */
    @Override
    protected int getMinimalNumberOfSegments() {
        return 5;
    }

    /**
     * Gets this UID instance.
     *
     * <p>
     * This method provides access to the UID instance itself, maintaining
     * consistency with the {@link HomekitServiceUID} interface.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     *   <li>Returns this instance</li>
     *   <li>Supports interface compliance</li>
     *   <li>Enables UID access</li>
     * </ul>
     *
     * @return This UID instance
     */
    @Override
    public HomekitServiceUID getUID() {
        return this;
    }
}
