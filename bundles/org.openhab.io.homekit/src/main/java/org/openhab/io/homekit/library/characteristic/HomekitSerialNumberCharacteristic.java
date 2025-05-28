package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitReadOnlyStringCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HomeKit Serial Number Characteristic.
 * 
 * <p>
 * This characteristic represents the serial number of a device.
 * It is a read-only string value that uniquely identifies the device instance.
 * </p>
 *
 * <p>
 * The serial number is used to:
 * <ul>
 * <li>Uniquely identify a specific instance of a device</li>
 * <li>Track and manage individual devices</li>
 * <li>Enable proper device registration and management</li>
 * <li>Support warranty and support services</li>
 * </ul>
 * </p>
 *
 * @author Karel Goderis - Initial Contribution
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "00000030-0000-1000-8000-0026BB765291", name = "Serial Number", tag = "serialNumber", acceptedItemTypes = {
        "String", "Text" })
@NonNullByDefault
public class HomekitSerialNumberCharacteristic extends HomekitReadOnlyStringCharacteristic {
    // ========== Log Message Prefixes ==========
    private static final String LOG_PREFIX = "Homekit SerialNumberCharacteristic: ";
    private static final String LOG_INIT = LOG_PREFIX + "Init - ";

    private final Logger logger = LoggerFactory.getLogger(HomekitSerialNumberCharacteristic.class);

    /**
     * Creates a new Serial Number characteristic.
     * 
     * <p>
     * This characteristic is read-only and provides the unique serial number of the accessory.
     * </p>
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param instanceId The instance ID for this characteristic
     */
    public HomekitSerialNumberCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(false).withEvents(false)
                .withDescription("Serial Number");
        logger.debug("{}Created new Serial Number characteristic with instance ID {}", LOG_INIT, instanceId);
    }

    /**
     * Creates a new Serial Number characteristic from a JSON value.
     * 
     * <p>
     * This constructor is used when restoring a characteristic from persistent storage.
     * </p>
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param value The JSON value to initialize the characteristic with
     */
    public HomekitSerialNumberCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
        logger.debug("{}Restored Serial Number characteristic from JSON", LOG_INIT);
    }
}
