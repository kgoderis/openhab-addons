package org.openhab.io.homekit.core.service;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.uid.HomekitServiceUID;
import org.openhab.io.homekit.util.HomekitUID;

/**
 * Implementation of a unique identifier for a HomeKit service.
 *
 * <p>
 * This class provides a structured way to identify HomeKit services within the system.
 * The UID format follows the pattern: homekit:service:{pairingId}:{accessoryId}:{serviceId}
 * where:
 * <ul>
 *   <li>pairingId: The hexadecimal pre-generated ID of the accessory</li>
 *   <li>accessoryId: The instance ID of the accessory</li>
 *   <li>serviceId: The instance ID of the service</li>
 * </ul>
 * </p>
 *
 * <p>
 * The class integrates with:
 * <ul>
 *   <li>{@link HomekitUID} for base UID functionality</li>
 *   <li>{@link HomekitServiceUID} for service-specific UID operations</li>
 *   <li>{@link org.openhab.core.thing.UID OpenHAB's UID system} for unique identification</li>
 * </ul>
 * </p>
 *
 * @author Karel Goderis - Initial contribution
 * @version 1.0
 * @since 1.0
 */
@NonNullByDefault
public class HomekitServiceUIDImpl extends HomekitUID implements HomekitServiceUID {
    private final long instanceId;

    // server id : accessory instance id : service id

    @Override
    protected int getMinimalNumberOfSegments() {
        return 5; // homekit:service:pairingId:accessoryId:serviceId
    }

    /**
     * Creates a new service UID with the specified components.
     *
     * This constructor builds a complete service UID from its constituent parts.
     * The resulting UID will be in the format: homekit:service:{pairingId}:{accessoryId}:{serviceId}
     *
     * @param pairingId The hexadecimal pre-generated ID of the accessory
     * @param accessoryId The accessory instance ID
     * @param serviceId The service instance ID
     */
    public HomekitServiceUIDImpl(String pairingId, long accessoryId, long serviceId) {
        super("service", "homekit:service:" + pairingId + ":" + accessoryId + ":" + serviceId);
        this.instanceId = accessoryId;
    }

    /**
     * Creates a new service UID from a complete UID string.
     *
     * This constructor parses an existing UID string into its components.
     * The UID string must follow the format: homekit:service:{pairingId}:{accessoryId}:{serviceId}
     *
     * @param key The complete UID string to parse
     */
    public HomekitServiceUIDImpl(String key) {
        super("service", key);
        this.instanceId = 0;
    }

    /**
     * Gets the service ID component of this UID.
     *
     * This method extracts the service ID from the UID segments.
     * The service ID is the last segment in the UID string.
     *
     * @return The service ID as a string
     */
    public String getServiceId() {
        return getSegment(4);
    }

    /**
     * Gets the complete UID as a string.
     *
     * @return The UID string representation
     */
    @Override
    public String getAsString() {
        return toString();
    }

    /**
     * Gets the instance ID of this service.
     *
     * @return The service instance ID
     */
    @Override
    public long getInstanceId() {
        return instanceId;
    }

    /**
     * Gets this UID instance.
     *
     * @return This UID instance
     */
    @Override
    public HomekitServiceUID getUID() {
        return this;
    }

    // /**
    // * Returns the id.
    // *
    // * @return id the id
    // */
    // public String getId() {
    // List<String> segments = getAllSegments();
    // return segments.get(segments.size() - 1);
    // }
}
