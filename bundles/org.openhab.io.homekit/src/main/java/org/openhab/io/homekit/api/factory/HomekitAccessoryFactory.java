package org.openhab.io.homekit.api.factory;

import java.util.Set;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.exception.HomekitFactoryException;

/**
 * Factory interface for creating HomeKit accessories.
 *
 * This interface defines the contract for factories that create HomeKit
 * accessories. It provides
 * methods for creating accessories based on type, tag, or configuration, and
 * for querying
 * supported accessory types and tags.
 *
 * The factory provides:
 * - Type-based accessory creation
 * - Tag-based accessory creation
 * - Configuration-based accessory creation
 * - Support validation
 * - Type and tag discovery
 *
 * Key implementation details:
 * - Thread-safe accessory creation
 * - Type-safe factory methods
 * - Support for variable arguments
 * - JSON configuration support
 * - Extensible type system
 *
 * The interface integrates with:
 * - {@link org.openhab.io.homekit.api.accessory.HomekitAccessory} for accessory
 * instances
 * - {@link javax.json.JsonValue} for configuration data
 *
 * @author Karel Goderis - Initial Contribution
 * @since 1.0.0
 */
@NonNullByDefault
public interface HomekitAccessoryFactory {
    /**
     * Creates a new accessory instance of the specified type.
     * This method creates a basic accessory with default configuration.
     *
     * @param type the accessory type identifier
     * @return a new accessory instance
     * @throws HomekitFactoryException if the accessory type is not supported or
     *             creation fails
     * @since 1.0.0
     */
    HomekitAccessory createAccessory(String type) throws HomekitFactoryException;

    /**
     * Creates a new accessory instance from a tag.
     * This method creates an accessory based on its tag identifier.
     *
     * @param tag the accessory tag
     * @return a new accessory instance
     * @throws HomekitFactoryException if the tag is not supported or creation fails
     * @since 1.0.0
     */
    HomekitAccessory createAccessoryFromTag(String tag) throws HomekitFactoryException;

    /**
     * Creates a new accessory instance with variable arguments.
     * This method allows passing additional configuration to the accessory
     * constructor.
     *
     * @param type the accessory type identifier
     * @param args the constructor arguments
     * @return a new accessory instance
     * @throws HomekitFactoryException if the accessory type is not supported or
     *             creation fails
     * @since 1.0.0
     */
    HomekitAccessory createAccessoryWithArgs(String type, Object... args) throws HomekitFactoryException;

    /**
     * Creates a new accessory instance using a tag from a JSON object.
     * This method creates an accessory with configuration from a JSON value.
     *
     * @param tag the accessory tag
     * @param value the accessory value
     * @return a new accessory instance
     * @throws HomekitFactoryException if the tag is not supported or creation fails
     * @since 1.0.0
     */
    HomekitAccessory createAccessoryFromTagWithValue(String tag, JsonValue value) throws HomekitFactoryException;

    /**
     * Checks if the factory supports creating accessories of the given type.
     * This method validates whether the factory can create accessories of a
     * specific type.
     *
     * @param type the accessory type to check
     * @return true if the type is supported, false otherwise
     * @since 1.0.0
     */
    boolean supportsAccessoryType(String type);

    /**
     * Checks if the factory supports creating accessories with the given tag.
     * This method validates whether the factory can create accessories with a
     * specific tag.
     *
     * @param tag the accessory tag to check
     * @return true if the tag is supported, false otherwise
     * @since 1.0.0
     */
    boolean supportsTag(String tag);

    /**
     * Returns a set of all supported accessory types.
     * This method provides a complete list of accessory types that can be created.
     *
     * @return an unmodifiable set of supported accessory types
     * @since 1.0.0
     */
    Set<String> getSupportedAccessoryTypes();

    /**
     * Returns a set of all supported accessory tags.
     * This method provides a complete list of accessory tags that can be used.
     *
     * @return an unmodifiable set of supported accessory tags
     * @since 1.0.0
     */
    Set<String> getSupportedTags();
}
