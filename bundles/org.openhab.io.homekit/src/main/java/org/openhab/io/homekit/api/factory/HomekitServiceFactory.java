package org.openhab.io.homekit.api.factory;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.service.HomekitService;

/**
 * Factory interface for creating HomeKit services.
 * This factory is responsible for creating service instances based on their type.
 */
@NonNullByDefault
public interface HomekitServiceFactory {
    /**
     * Creates a new HomeKit service instance.
     *
     * @param type The type of service to create
     * @param accessory The accessory that will own this service
     * @return A new service instance
     * @throws IllegalArgumentException if the service type is not supported
     */
    HomekitService createService(String type, HomekitAccessory accessory);

    /**
     * Creates a new HomeKit service instance based on a tag.
     * The tag is typically derived from the service class name by removing "HomekitService" suffix.
     *
     * @param tag The tag identifying the service (e.g. "LightBulb", "Switch", "Thermostat")
     * @param accessory The accessory that will own this service
     * @return A new service instance
     * @throws IllegalArgumentException if no service type is found for the given tag
     */
    HomekitService createServiceFromTag(String tag, HomekitAccessory accessory);

    /**
     * Creates a new HomeKit service instance with variable arguments.
     * This method allows for more flexible service creation by accepting any number of constructor arguments.
     *
     * @param type The type of service to create
     * @param args The constructor arguments
     * @return A new service instance
     * @throws IllegalArgumentException if the service type is not supported or creation fails
     */
    HomekitService createServiceWithArgs(String type, Object... args);

    /**
     * Checks if this factory supports creating a service of the given type.
     *
     * @param type The type of service to check
     * @return true if this factory can create services of the given type
     */
    boolean supportsServiceType(String type);

    /**
     * Checks if this factory supports creating a service with the given tag.
     *
     * @param tag The tag to check
     * @return true if this factory can create services with the given tag
     */
    boolean supportsTag(String tag);

    /**
     * Gets all service types supported by this factory.
     *
     * @return A set of supported service type identifiers
     */
    Set<String> getSupportedServiceTypes();

    /**
     * Gets all service tags supported by this factory.
     *
     * @return A set of supported service tags
     */
    Set<String> getSupportedTags();

    /**
     * Gets the tag from the service type.
     *
     * @param serviceType The service type
     * @return The tag
     */
    String getTagFromServiceType(String serviceType);

    /**
     * Gets the service type from the tag.
     *
     * @param serviceTag The tag
     * @return The service type
     */
    String getServiceTypeFromTag(String serviceTag);
}
