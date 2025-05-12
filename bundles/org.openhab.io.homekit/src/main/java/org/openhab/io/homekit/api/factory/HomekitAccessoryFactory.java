package org.openhab.io.homekit.api.factory;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * Factory interface for creating HomeKit accessories.
 * This factory is responsible for creating accessory instances based on their type or tag.
 */
@NonNullByDefault
public interface HomekitAccessoryFactory {
    /**
     * Creates a new accessory instance of the specified type.
     *
     * @param type the accessory type identifier
     * @param eventManager the event manager to use for the accessory
     * @return a new accessory instance
     * @throws IllegalArgumentException if the accessory type is not supported or creation fails
     */
    HomekitAccessory createAccessory(String type, HomekitEventManager eventManager);

    /**
     * Creates a new accessory instance from a tag.
     *
     * @param tag the accessory tag
     * @param eventManager the event manager to use for the accessory
     * @return a new accessory instance
     * @throws IllegalArgumentException if the tag is not supported or creation fails
     */
    HomekitAccessory createAccessoryFromTag(String tag, HomekitEventManager eventManager);

    /**
     * Creates a new accessory instance with variable arguments.
     *
     * @param type the accessory type identifier
     * @param args the constructor arguments
     * @return a new accessory instance
     * @throws IllegalArgumentException if the accessory type is not supported or creation fails
     */
    HomekitAccessory createAccessoryWithArgs(String type, Object... args);

    /**
     * Checks if the factory supports creating accessories of the given type.
     *
     * @param type the accessory type to check
     * @return true if the type is supported, false otherwise
     */
    boolean supportsAccessoryType(String type);

    /**
     * Checks if the factory supports creating accessories with the given tag.
     *
     * @param tag the accessory tag to check
     * @return true if the tag is supported, false otherwise
     */
    boolean supportsTag(String tag);

    /**
     * Returns a set of all supported accessory types.
     *
     * @return an unmodifiable set of supported accessory types
     */
    Set<String> getSupportedAccessoryTypes();

    /**
     * Returns a set of all supported accessory tags.
     *
     * @return an unmodifiable set of supported accessory tags
     */
    Set<String> getSupportedTags();
}
