package org.openhab.io.homekit.api.factory;

/**
 * Enum representing the different types of factories in the HomeKit integration.
 *
 * This enum defines the types of components that can be created by factories in the HomeKit
 * integration. Each type corresponds to a specific factory interface that is responsible for
 * creating instances of that component type.
 *
 * The factory types are:
 * - ACCESSORY: Creates HomeKit accessories
 * - SERVICE: Creates HomeKit services
 * - CHARACTERISTIC: Creates HomeKit characteristics
 *
 * Key implementation details:
 * - Used for factory registration and lookup
 * - Supports component type identification
 * - Enables factory type validation
 * - Facilitates factory management
 *
 * @author Karel Goderis - Initial Contribution
 * @since 1.0.0
 */
public enum HomekitFactoryType {
    /**
     * Factory type for creating HomeKit accessories.
     * Used by {@link org.openhab.io.homekit.api.factory.HomekitAccessoryFactory}.
     */
    ACCESSORY,

    /**
     * Factory type for creating HomeKit services.
     * Used by {@link org.openhab.io.homekit.api.factory.HomekitServiceFactory}.
     */
    SERVICE,

    /**
     * Factory type for creating HomeKit characteristics.
     * Used by {@link org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory}.
     */
    CHARACTERISTIC
}
