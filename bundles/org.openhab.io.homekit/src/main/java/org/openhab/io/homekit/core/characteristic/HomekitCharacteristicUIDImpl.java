package org.openhab.io.homekit.core.characteristic;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.uid.HomekitCharacteristicUID;
import org.openhab.io.homekit.util.HomekitUID;
import java.util.List;

/**
 * A unique identifier implementation for HomeKit characteristics that follows the HomeKit Accessory Protocol (HAP) specification.
 * This class extends {@link HomekitUID} and implements {@link HomekitCharacteristicUID} to provide a structured way to identify
 * and manage HomeKit characteristics within the OpenHAB system.
 *
 * <p>
 * The UID format follows the pattern: homekit:characteristic:{pairingId}:{accessoryId}:{serviceId}:{characteristicId}
 * where:
 * <ul>
 *   <li>pairingId: The hexadecimal pre-generated ID of the accessory</li>
 *   <li>accessoryId: The instance ID of the accessory</li>
 *   <li>serviceId: The instance ID of the service</li>
 *   <li>characteristicId: The instance ID of the characteristic</li>
 * </ul>
 * </p>
 *
 * <p>
 * This implementation integrates with several key components:
 * <ul>
 *   <li>{@link HomekitUID} - Provides the base UID functionality and validation</li>
 *   <li>{@link HomekitCharacteristicUID} - Defines the characteristic-specific UID operations</li>
 *   <li>{@link org.openhab.core.thing.UID} - Integrates with OpenHAB's UID system for unique identification</li>
 *   <li>{@link HomekitCharacteristic} - Links to the characteristic implementation</li>
 *   <li>{@link HomekitService} - Connects to the service that contains this characteristic</li>
 * </ul>
 * </p>
 *
 * <p>
 * The class is designed to work seamlessly with the HomeKit Accessory Protocol (HAP) specification, which can be found at:
 * <a href="https://developer.apple.com/documentation/HomeKit">https://developer.apple.com/documentation/HomeKit</a>
 * </p>
 *
 * @author Karel Goderis - Initial contribution
 * @version 1.0
 * @since 1.0
 */
@NonNullByDefault
public class HomekitCharacteristicUIDImpl extends HomekitUID implements HomekitCharacteristicUID {
    private final long instanceId;

    /**
     * Creates a new characteristic UID with the specified components. This constructor is used when creating
     * a new characteristic identifier from its constituent parts.
     *
     * @param pairingId the hexadecimal pre-generated id of the accessory
     * @param accessoryId the accessory instance id
     * @param serviceId the service instance id
     * @param characteristicId the characteristic instance id
     * @throws IllegalArgumentException if any of the parameters are invalid
     */
    public HomekitCharacteristicUIDImpl(String pairingId, long accessoryId, long serviceId, long characteristicId) {
        super("characteristic",
                "homekit:characteristic:" + pairingId + ":" + accessoryId + ":" + serviceId + ":" + characteristicId);
        this.instanceId = accessoryId;
    }

    /**
     * Creates a new characteristic UID from an existing UID string. This constructor is used when parsing
     * an existing UID string into a characteristic identifier.
     *
     * @param key the UID string to parse
     * @throws IllegalArgumentException if the UID string is invalid
     */
    public HomekitCharacteristicUIDImpl(String key) {
        super("characteristic", key);
        this.instanceId = 0;
    }

    /**
     * Returns the minimum number of segments required for a valid characteristic UID.
     * This is used for validation purposes.
     *
     * @return the minimum number of segments (6 for homekit:characteristic:pairingId:accessoryId:serviceId:characteristicId)
     */
    @Override
    protected int getMinimalNumberOfSegments() {
        return 6; // homekit:characteristic:pairingId:accessoryId:serviceId:characteristicId
    }

    /**
     * Returns the HomeKit ID part of this characteristic UID. This method extracts the last 4 segments
     * of the UID and joins them with the separator to form the HomeKit-specific identifier.
     *
     * @return The HomeKit ID in the format: pairingId:accessoryId:serviceId:characteristicId
     */
    public String getHomekitId() {
        return String.join(SEPARATOR, getAllSegments().subList(getAllSegments().size() - 4, getAllSegments().size()));
    }

    /**
     * Returns the string representation of this characteristic UID. This method is used for
     * serialization and display purposes.
     *
     * @return the string representation of this UID
     */
    @Override
    public String getAsString() {
        return toString();
    }

    /**
     * Returns the instance ID of this characteristic. This is used to uniquely identify
     * the characteristic within its service.
     *
     * @return the instance ID of this characteristic
     */
    @Override
    public long getInstanceId() {
        return instanceId;
    }

    /**
     * Returns this characteristic UID. This method is used to maintain consistency with
     * the {@link HomekitCharacteristicUID} interface.
     *
     * @return this characteristic UID
     */
    @Override
    public HomekitCharacteristicUID getUID() {
        return this;
    }

    /**
     * Returns a string representation of this characteristic UID. This method overrides
     * the default implementation from {@link Object} to provide a formatted string
     * representation of the UID.
     *
     * @return a string representation of this characteristic UID
     */
    @Override
    public String toString() {
        return super.toString();
    }

    /**
     * Returns all segments of this characteristic UID. This method is used internally
     * to access the individual parts of the UID for operations like {@link #getHomekitId()}.
     * The segments are returned in order: [homekit, characteristic, pairingId, accessoryId, serviceId, characteristicId].
     *
     * @return a list of all segments in this UID
     */
    @Override
    protected List<String> getAllSegments() {
        return super.getAllSegments();
    }
}
