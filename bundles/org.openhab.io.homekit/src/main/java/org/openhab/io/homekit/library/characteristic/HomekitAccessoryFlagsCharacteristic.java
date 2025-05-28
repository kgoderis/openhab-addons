package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitLongCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HomeKit Accessory Flags Characteristic.
 * 
 * <p>
 * This characteristic represents the flags that describe the capabilities and state of an accessory.
 * The flags are represented as a 32-bit unsigned integer (0 to 0xFFFFFFFF).
 * </p>
 *
 * <p>
 * The flags can indicate various accessory states and capabilities, such as:
 * <ul>
 * <li>Whether the accessory is bridged</li>
 * <li>Whether the accessory supports software updates</li>
 * <li>Whether the accessory is discoverable</li>
 * <li>Whether the accessory is paired</li>
 * </ul>
 * </p>
 *
 * @author Karel Goderis - Initial Contribution
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "000000A6-0000-1000-8000-0026BB765291", name = "Accessory Flags", tag = "accessoryFlags", acceptedItemTypes = {
        "Number" })
@NonNullByDefault
public class HomekitAccessoryFlagsCharacteristic extends HomekitLongCharacteristic {
    // ========== Log Message Prefixes ==========
    private static final String LOG_PREFIX = "Homekit AccessoryFlagsCharacteristic: ";
    private static final String LOG_INIT = LOG_PREFIX + "Init - ";
    private static final String LOG_WARN = LOG_PREFIX + "Warning - ";

    private final Logger logger = LoggerFactory.getLogger(HomekitAccessoryFlagsCharacteristic.class);

    /**
     * Creates a new Accessory Flags characteristic.
     * 
     * <p>
     * This characteristic is read-only and provides information about the accessory's capabilities and state.
     * </p>
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param instanceId The instance ID for this characteristic
     */
    public HomekitAccessoryFlagsCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, 0L, 0xFFFFFFFFL, 1L);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(false).withEvents(true)
                .withDescription("Accessory Flags");
        logger.debug("{}Created new Accessory Flags characteristic with instance ID {}", LOG_INIT, instanceId);
    }

    /**
     * Creates a new Accessory Flags characteristic from a JSON value.
     * 
     * <p>
     * This constructor is used when restoring a characteristic from persistent storage.
     * </p>
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param value The JSON value to initialize the characteristic with
     */
    public HomekitAccessoryFlagsCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
        logger.debug("{}Restored Accessory Flags characteristic from JSON", LOG_INIT);
    }

    /**
     * Checks if a value is allowed for this characteristic.
     * 
     * <p>
     * The value must be between 0 and 0xFFFFFFFF (inclusive).
     * </p>
     *
     * @param value The value to check
     * @return true if the value is within the allowed range, false otherwise
     */
    @Override
    public boolean isAllowedValue(Long value) {
        boolean allowed = value != null && value >= 0L && value <= 0xFFFFFFFFL;
        if (!allowed) {
            logger.warn("{}Value {} is not allowed for Accessory Flags characteristic", LOG_WARN, value);
        }
        return allowed;
    }

    /**
     * Gets the set of allowed values for this characteristic.
     * 
     * <p>
     * Since this characteristic accepts any value in its range, returns an empty set.
     * </p>
     *
     * @return An empty set, indicating any value in the range is allowed
     */
    @Override
    public java.util.Set<Long> getAllowedValues() {
        return java.util.Collections.emptySet(); // No specific allowed values, just a range
    }
}
