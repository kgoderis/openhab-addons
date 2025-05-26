package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitStringCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HomeKit Manufacturer Characteristic.
 * 
 * <p>
 * This characteristic represents the manufacturer name of the accessory.
 * It is a read-only string value that identifies the company that made the device.
 * </p>
 *
 * <p>
 * The manufacturer name is used to:
 * <ul>
 *   <li>Identify the company that produced the accessory</li>
 *   <li>Help users identify and organize their accessories</li>
 *   <li>Provide context for support and troubleshooting</li>
 * </ul>
 * </p>
 *
 * @author Karel Goderis - Initial Contribution
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "00000020-0000-1000-8000-0026BB765291", name = "Manufacturer", tag = "manufacturer", acceptedItemTypes = {
        "String", "Text" })
@NonNullByDefault
public class HomekitManufacturerCharacteristic extends HomekitStringCharacteristic {
    // ========== Log Message Prefixes ==========
    private static final String LOG_PREFIX = "Homekit ManufacturerCharacteristic: ";
    private static final String LOG_INIT = LOG_PREFIX + "Init - ";

    private final Logger logger = LoggerFactory.getLogger(HomekitManufacturerCharacteristic.class);

    /**
     * Creates a new Manufacturer characteristic.
     * 
     * <p>
     * This characteristic is read-only and provides the manufacturer name of the accessory.
     * </p>
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param instanceId The instance ID for this characteristic
     */
    public HomekitManufacturerCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(false)
                .withDescription("Manufacturer");
        logger.debug("{}Created new Manufacturer characteristic with instance ID {}", LOG_INIT, instanceId);
    }

    /**
     * Creates a new Manufacturer characteristic from a JSON value.
     * 
     * <p>
     * This constructor is used when restoring a characteristic from persistent storage.
     * </p>
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param value The JSON value to initialize the characteristic with
     */
    public HomekitManufacturerCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
        logger.debug("{}Restored Manufacturer characteristic from JSON", LOG_INIT);
    }
}
