package org.openhab.io.homekit.api.factory;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristic;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.exception.HomekitFactoryException;

/**
 * Factory interface for creating HomeKit characteristics.
 *
 * This interface defines the contract for factories that create HomeKit
 * characteristics. It provides
 * methods for creating characteristics based on type or tag, and for querying
 * supported types and
 * their associated metadata.
 *
 * The factory provides:
 * - Type-based characteristic creation
 * - Tag-based characteristic creation
 * - Service integration
 * - Type and tag discovery
 * - Item type mapping
 *
 * Key implementation details:
 * - Thread-safe characteristic creation
 * - Type-safe factory methods
 * - Support for variable arguments
 * - Service ownership management
 * - Tag-based type resolution
 *
 * The interface integrates with:
 * - {@link org.openhab.io.homekit.api.characteristic.HomekitCharacteristic} for
 * characteristic instances
 * - {@link org.openhab.io.homekit.api.service.HomekitService} for service
 * integration
 *
 * @author Karel Goderis - Initial Contribution
 * @since 1.0.0
 */
@NonNullByDefault
public interface HomekitCharacteristicFactory {
    /**
     * Creates a new HomeKit characteristic instance.
     * This method creates a basic characteristic with default configuration.
     *
     * @param type The type of characteristic to create
     * @param service The service that will own this characteristic
     * @return A new characteristic instance
     * @throws HomekitFactoryException if the characteristic type is not
     *             supported
     * @since 1.0.0
     */
    HomekitCharacteristic<?> createCharacteristic(String type, HomekitService service) throws HomekitFactoryException;

    /**
     * Creates a new HomeKit characteristic instance based on a tag.
     * The tag is typically derived from the characteristic class name by removing
     * "HomekitCharacteristic" suffix.
     *
     * @param tag The tag identifying the characteristic (e.g. "On",
     *            "Brightness", "Temperature")
     * @param service The service that will own this characteristic
     * @return A new characteristic instance
     * @throws HomekitFactoryException if no characteristic type is found for the
     *             given tag
     * @since 1.0.0
     */
    HomekitCharacteristic<?> createCharacteristicFromTag(String tag, HomekitService service)
            throws HomekitFactoryException;

    /**
     * Creates a new HomeKit characteristic instance with variable arguments.
     * This method allows for more flexible characteristic creation by accepting any
     * number of constructor arguments.
     *
     * @param type The type of characteristic to create
     * @param args The constructor arguments
     * @return A new characteristic instance
     * @throws HomekitFactoryException if the characteristic type is not
     *             supported or creation fails
     * @since 1.0.0
     */
    HomekitCharacteristic<?> createCharacteristicWithArgs(String type, Object... args) throws HomekitFactoryException;

    /**
     * Checks if this factory supports creating a characteristic of the given type.
     * This method validates whether the factory can create characteristics of a
     * specific type.
     *
     * @param type The type of characteristic to check
     * @return true if this factory can create characteristics of the given type
     * @since 1.0.0
     */
    boolean supportsCharacteristicType(String type);

    /**
     * Checks if this factory supports creating a characteristic with the given tag.
     * This method validates whether the factory can create characteristics with a
     * specific tag.
     *
     * @param tag The tag to check
     * @return true if this factory can create characteristics with the given tag
     * @since 1.0.0
     */
    boolean supportsTag(String tag);

    /**
     * Gets all characteristic types supported by this factory.
     * This method provides a complete list of characteristic types that can be
     * created.
     *
     * @return A set of supported characteristic type identifiers
     * @since 1.0.0
     */
    java.util.Set<String> getSupportedCharacteristicTypes();

    /**
     * Gets all characteristic tags supported by this factory.
     * This method provides a complete list of characteristic tags that can be used.
     *
     * @return A set of supported characteristic tags
     * @since 1.0.0
     */
    java.util.Set<String> getSupportedTags();

    /**
     * Gets the tag from the characteristic type.
     * This method converts a characteristic type to its corresponding tag.
     *
     * @param characteristicType The characteristic type
     * @return The tag
     * @throws HomekitFactoryException if the characteristic type is not supported
     * @since 1.0.0
     */
    String getTagFromCharacteristicType(String characteristicType) throws HomekitFactoryException;

    /**
     * Gets the service type from the tag.
     * This method converts a characteristic tag to its corresponding type.
     *
     * @param characteristicTag The tag
     * @return The characteristic type
     * @throws HomekitFactoryException if the tag is not supported
     * @since 1.0.0
     */
    String getCharacteristicTypeFromTag(String characteristicTag) throws HomekitFactoryException;

    /**
     * Gets the accepted item types for the characteristic type.
     * This method provides the list of OpenHAB item types that can be used with
     * this characteristic.
     *
     * @param characteristicType The characteristic type
     * @return The accepted item types
     * @throws HomekitFactoryException if the characteristic type is not supported
     * @since 1.0.0
     */
    Set<String> getAcceptedItemTypes(String characteristicType) throws HomekitFactoryException;
}
