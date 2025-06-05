package org.openhab.io.homekit.api.factory;

import java.util.Map;
import java.util.Set;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.exception.HomekitFactoryException;

/**
 * Factory interface for creating HomeKit services.
 * This factory is responsible for creating and managing HomeKit service
 * instances
 * based on service types, tags, or configuration.
 *
 * <p>
 * The factory provides methods for:
 * <ul>
 * <li>Creating services from service types</li>
 * <li>Creating services from service tags</li>
 * <li>Creating services with variable arguments</li>
 * <li>Creating services from JSON configuration</li>
 * <li>Querying supported service types and tags</li>
 * <li>Retrieving characteristic types for services</li>
 * </ul>
 * </p>
 *
 * <p>
 * Service types and tags are used to identify different kinds of HomeKit
 * services.
 * Each service type is associated with a set of mandatory and optional
 * characteristics
 * that define its behavior and capabilities.
 * </p>
 *
 * @author Karel Goderis - Initial contribution
 * @version 1.0
 * @since 1.0
 */
@NonNullByDefault
public interface HomekitServiceFactory {
    /**
     * Creates a new service instance for the specified type and accessory.
     *
     * <p>
     * The service is created using the standard constructor that takes an accessory
     * and event manager. The service type must be previously registered during
     * initialization.
     * </p>
     *
     * @param type The service type to create
     * @param accessory The accessory that will own the service
     * @return A new service instance
     * @throws HomekitFactoryException if the service type is not supported or
     *             creation fails
     * @since 1.0
     */
    HomekitService createService(String type, HomekitAccessory accessory) throws HomekitFactoryException;

    /**
     * Creates a service instance with variable arguments.
     * This method allows for more flexible service creation by accepting any number
     * of constructor arguments.
     *
     * <p>
     * The method will:
     * <ol>
     * <li>Determine the appropriate constructor based on argument types</li>
     * <li>Create a new service instance using the constructor</li>
     * <li>Return the created service</li>
     * </ol>
     * </p>
     *
     * @param type The service type to create
     * @param args The constructor arguments
     * @return A new service instance
     * @throws HomekitFactoryException if the service type is not supported or
     *             creation fails
     * @since 1.0
     */
    HomekitService createServiceWithArgs(String type, Object... args) throws HomekitFactoryException;

    /**
     * Creates a service instance from a service tag.
     *
     * <p>
     * The method will:
     * <ol>
     * <li>Look up the service type for the given tag</li>
     * <li>Create a new service instance using the found type</li>
     * <li>Return the created service</li>
     * </ol>
     * </p>
     *
     * @param tag The service tag to create from
     * @param accessory The accessory that will own the service
     * @return A new service instance
     * @throws HomekitFactoryException if the tag is not supported or service
     *             creation fails
     * @since 1.0
     */
    HomekitService createServiceFromTag(String tag, HomekitAccessory accessory) throws HomekitFactoryException;

    /**
     * Checks if the factory supports a specific service type.
     *
     * <p>
     * This method checks if the factory has registered the given service type
     * and can create instances of it.
     * </p>
     *
     * @param type The service type to check
     * @return true if the type is supported, false otherwise
     * @since 1.0
     */
    boolean supportsServiceType(String type);

    /**
     * Checks if the factory supports creating services with the given tag.
     *
     * <p>
     * This method checks if the factory has registered the given service tag
     * and can create instances using it.
     * </p>
     *
     * @param tag The service tag to check
     * @return true if the tag is supported, false otherwise
     * @since 1.0
     */
    boolean supportsTag(String tag);

    /**
     * Returns a set of all supported service tags.
     *
     * <p>
     * The returned set contains all service tags that have been registered
     * with the factory and can be used to create services.
     * </p>
     *
     * @return An unmodifiable set of supported service tags
     * @since 1.0
     */
    Set<String> getSupportedTags();

    /**
     * Returns a set of all supported service types.
     *
     * <p>
     * The returned set contains all service types that have been registered
     * with the factory and can be used to create services.
     * </p>
     *
     * @return An unmodifiable set of supported service types
     * @since 1.0
     */
    Set<String> getSupportedServiceTypes();

    /**
     * Creates a service instance from a JSON value.
     * This method extracts the service type from the JSON value and creates the
     * appropriate service.
     *
     * <p>
     * The JSON value must contain:
     * <ul>
     * <li>A "type" field with the service type</li>
     * <li>Any additional configuration required by the service</li>
     * </ul>
     * </p>
     *
     * @param accessory The accessory that will own the service
     * @param value The JSON value containing service configuration
     * @return A new service instance
     * @throws HomekitFactoryException if the JSON value is invalid or service
     *             creation fails
     * @since 1.0
     */
    HomekitService createServiceWithValue(HomekitAccessory accessory, JsonValue value) throws HomekitFactoryException;

    /**
     * Gets the service tag for a given service type.
     *
     * <p>
     * This method looks up the service tag that corresponds to the given
     * service type in the factory's registry.
     * </p>
     *
     * @param serviceType The service type to get the tag for
     * @return The service tag
     * @throws HomekitFactoryException if the service type is not supported
     * @since 1.0
     */
    String getTagFromServiceType(String serviceType) throws HomekitFactoryException;

    /**
     * Gets the service type for a given service tag.
     *
     * <p>
     * This method looks up the service type that corresponds to the given
     * service tag in the factory's registry.
     * </p>
     *
     * @param serviceTag The service tag to get the type for
     * @return The service type
     * @throws HomekitFactoryException if the service tag is not supported
     * @since 1.0
     */
    String getServiceTypeFromTag(String serviceTag) throws HomekitFactoryException;

    /**
     * Gets the mandatory and optional characteristic types for a given service
     * type.
     *
     * <p>
     * The returned map contains two sets:
     * <ul>
     * <li>"mandatory" - Set of required characteristic types</li>
     * <li>"optional" - Set of optional characteristic types</li>
     * </ul>
     * </p>
     *
     * @param serviceType The service type to get characteristics for
     * @return A map containing sets of mandatory and optional characteristic types
     * @throws HomekitFactoryException if the service type is not supported
     * @since 1.0
     */
    Map<String, Set<String>> getCharacteristicTypes(String serviceType) throws HomekitFactoryException;
}
