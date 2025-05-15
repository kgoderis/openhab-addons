package org.openhab.io.homekit.api.factory;

import java.util.Set;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;

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
     * @return a new accessory instance
     * @throws IllegalArgumentException if the accessory type is not supported or creation fails
     */
    HomekitAccessory createAccessory(String type);

    /**
     * Creates a new accessory instance from a tag.
     *
     * @param tag the accessory tag
     * @return a new accessory instance
     * @throws IllegalArgumentException if the tag is not supported or creation fails
     */
    HomekitAccessory createAccessoryFromTag(String tag);

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
     * Creates a new accessory instance using a tag from a JSON object.
     *
     * @param tag the accessory tag
     * @param value the accessory value
     * @return a new accessory instance
     * @throws IllegalArgumentException if the tag is not supported or creation fails
     */
    HomekitAccessory createAccessoryFromTagWithValue(String tag, JsonValue value);

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
