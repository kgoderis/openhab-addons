package org.openhab.io.homekit.api.factory;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristic;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * Factory interface for creating HomeKit characteristics.
 * This factory is responsible for creating characteristic instances based on their type.
 */
@NonNullByDefault
public interface HomekitCharacteristicFactory {
    /**
     * Creates a new HomeKit characteristic instance.
     *
     * @param type The type of characteristic to create
     * @param service The service that will own this characteristic
     * @param eventManager The event manager for handling characteristic events
     * @return A new characteristic instance
     * @throws IllegalArgumentException if the characteristic type is not supported
     */
    HomekitCharacteristic<?> createCharacteristic(String type, HomekitService service,
            HomekitEventManager eventManager);

    /**
     * Creates a new HomeKit characteristic instance based on a tag.
     * The tag is typically derived from the characteristic class name by removing "HomekitCharacteristic" suffix.
     *
     * @param tag The tag identifying the characteristic (e.g. "On", "Brightness", "Temperature")
     * @param service The service that will own this characteristic
     * @param eventManager The event manager for handling characteristic events
     * @return A new characteristic instance
     * @throws IllegalArgumentException if no characteristic type is found for the given tag
     */
    HomekitCharacteristic<?> createCharacteristicFromTag(String tag, HomekitService service,
            HomekitEventManager eventManager);

    /**
     * Creates a new HomeKit characteristic instance with variable arguments.
     * This method allows for more flexible characteristic creation by accepting any number of constructor arguments.
     *
     * @param type The type of characteristic to create
     * @param args The constructor arguments
     * @return A new characteristic instance
     * @throws IllegalArgumentException if the characteristic type is not supported or creation fails
     */
    HomekitCharacteristic<?> createCharacteristicWithArgs(String type, Object... args);

    /**
     * Checks if this factory supports creating a characteristic of the given type.
     *
     * @param type The type of characteristic to check
     * @return true if this factory can create characteristics of the given type
     */
    boolean supportsCharacteristicType(String type);

    /**
     * Checks if this factory supports creating a characteristic with the given tag.
     *
     * @param tag The tag to check
     * @return true if this factory can create characteristics with the given tag
     */
    boolean supportsTag(String tag);

    /**
     * Gets all characteristic types supported by this factory.
     *
     * @return A set of supported characteristic type identifiers
     */
    java.util.Set<String> getSupportedCharacteristicTypes();

    /**
     * Gets all characteristic tags supported by this factory.
     *
     * @return A set of supported characteristic tags
     */
    java.util.Set<String> getSupportedTags();
}
