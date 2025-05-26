package org.openhab.io.homekit.core.accessory;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.accessory.HomekitAccessoryType;
import org.openhab.io.homekit.api.factory.HomekitAccessoryFactory;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.factory.HomekitServiceFactory;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.service.AbstractHomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A generic HomeKit accessory implementation that can be used as a base for various types of accessories.
 * This class provides a flexible foundation for creating custom HomeKit accessories with standard functionality.
 *
 * <p>
 * The generic accessory supports:
 * <ul>
 *   <li>Basic accessory information (name, manufacturer, model, serial number)</li>
 *   <li>Standard HomeKit services and characteristics</li>
 *   <li>Event handling and state management</li>
 *   <li>JSON serialization for persistence</li>
 * </ul>
 * </p>
 *
 * <p>
 * This class is particularly useful when:
 * <ul>
 *   <li>Creating new types of HomeKit accessories</li>
 *   <li>Implementing custom accessory behavior</li>
 *   <li>Extending basic HomeKit functionality</li>
 * </ul>
 * </p>
 *
 * @author Karel Goderis - Initial contribution
 * @version 1.0
 * @since 1.0
 */
@NonNullByDefault
@HomekitAccessoryType(name = "Generic Accessory", type = "1110001-0000-1000-8000-0026BB765291", tag = "generic")
public class HomekitGenericAccessory extends AbstractHomekitAccessory {
    // ========== Log Message Prefixes ==========
    private static final String LOG_PREFIX = "Homekit GenericAccessory: ";
    private static final String LOG_INIT = LOG_PREFIX + "Init - ";

    private final Logger logger = LoggerFactory.getLogger(HomekitGenericAccessory.class);

    /**
     * Creates a new generic HomeKit accessory with the specified factories and event manager.
     * This constructor initializes a new accessory with default settings.
     *
     * @param eventManager The event manager for handling HomeKit events
     * @param serviceFactory The factory for creating HomeKit services
     * @param characteristicFactory The factory for creating HomeKit characteristics
     */
    public HomekitGenericAccessory(HomekitEventManager eventManager, HomekitServiceFactory serviceFactory,
            HomekitCharacteristicFactory characteristicFactory) {
        super(eventManager, serviceFactory, characteristicFactory);
        logger.debug("{}Created new generic accessory", LOG_INIT);
    }

    /**
     * Creates a new generic HomeKit accessory from a JSON value.
     * This constructor is used when restoring an accessory from persistent storage.
     *
     * @param eventManager The event manager for handling HomeKit events
     * @param serviceFactory The factory for creating HomeKit services
     * @param characteristicFactory The factory for creating HomeKit characteristics
     * @param value The JSON value containing the accessory configuration
     */
    public HomekitGenericAccessory(HomekitEventManager eventManager, HomekitServiceFactory serviceFactory,
            HomekitCharacteristicFactory characteristicFactory, JsonValue value) {
        super(eventManager, serviceFactory, characteristicFactory, value);
        logger.debug("{}Restored generic accessory from JSON", LOG_INIT);
    }
}
