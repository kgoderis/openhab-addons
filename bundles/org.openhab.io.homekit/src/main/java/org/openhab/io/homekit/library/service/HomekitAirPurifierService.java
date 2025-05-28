package org.openhab.io.homekit.library.service;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.service.HomekitServiceType;
import org.openhab.io.homekit.core.service.AbstractHomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.library.characteristic.HomekitActiveCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitCurrentAirPurifierStateCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitLockPhysicalControlsCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitNameCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitRotationSpeedCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitSwingModeCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitTargetAirPurifierStateCharacteristic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HomeKit Air Purifier Service.
 * 
 * <p>
 * This service provides control over air purifiers and their settings, including:
 * <ul>
 * <li>Power state management</li>
 * <li>Purification mode control</li>
 * <li>Fan speed adjustment</li>
 * <li>Swing mode operation</li>
 * </ul>
 * </p>
 *
 * <p>
 * The service is used to:
 * <ul>
 * <li>Control air purifier operation</li>
 * <li>Monitor purification states</li>
 * <li>Adjust fan speeds</li>
 * <li>Manage physical control locks</li>
 * <li>Configure swing modes</li>
 * </ul>
 * </p>
 *
 * <p>
 * For more information, see the
 * <a href="https://developer.apple.com/documentation/HomeKit">HomeKit Accessory Protocol Specification</a>.
 * </p>
 *
 * @author Karel Goderis - Initial contribution
 * @version 1.0
 * @since 1.0
 */
@HomekitServiceType(type = "000000BB-0000-1000-8000-0026BB765291", name = "Air Purifier", tag = "airPurifier")
@NonNullByDefault
public class HomekitAirPurifierService extends AbstractHomekitService {
    // ========== Log Message Prefixes ==========
    private static final String LOG_PREFIX = "Homekit AirPurifierService: ";
    private static final String LOG_INIT = LOG_PREFIX + "Init - ";
    private static final String LOG_STATE = LOG_PREFIX + "State - ";
    private static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    private static final String LOG_ACCESSORY = LOG_PREFIX + "HomekitAccessory - ";
    private static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    private static final String LOG_WARN = LOG_PREFIX + "Warning - ";

    private final Logger logger = LoggerFactory.getLogger(HomekitAirPurifierService.class);

    /**
     * Creates a new Air Purifier service.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @since 1.0
     */
    public HomekitAirPurifierService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory) {
        super(accessory, eventManager, characteristicFactory);
        withName("Air Purifier").withPrimary(false).withHidden(false);
        logger.debug("{}Created new Air Purifier service for accessory {}", LOG_INIT, accessory.getLabel());
    }

    /**
     * Creates a new Air Purifier service from a JSON configuration.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @param value JSON value containing service configuration
     * @since 1.0
     */
    public HomekitAirPurifierService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory, JsonValue value) {
        super(accessory, eventManager, characteristicFactory, value);
        logger.debug("{}Created new Air Purifier service from JSON configuration for accessory {}", LOG_INIT,
                accessory.getLabel());
    }

    /**
     * Adds the required and optional characteristics for this service.
     * 
     * <p>
     * Required characteristics:
     * <ul>
     * <li>Active (UUID: 000000B0-0000-1000-8000-0026BB765291)</li>
     * <li>CurrentAirPurifierState (UUID: 000000A9-0000-1000-8000-0026BB765291)</li>
     * <li>TargetAirPurifierState (UUID: 000000A8-0000-1000-8000-0026BB765291)</li>
     * </ul>
     * </p>
     *
     * <p>
     * Optional characteristics:
     * <ul>
     * <li>LockPhysicalControls (UUID: 000000A7-0000-1000-8000-0026BB765291)</li>
     * <li>Name (UUID: 00000023-0000-1000-8000-0026BB765291)</li>
     * <li>RotationSpeed (UUID: 00000029-0000-1000-8000-0026BB765291)</li>
     * <li>SwingMode (UUID: 000000B6-0000-1000-8000-0026BB765291)</li>
     * </ul>
     * </p>
     *
     * @since 1.0
     */
    @Override
    public void addCharacteristics() {
        addCharacteristic(
                new HomekitActiveCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(true));
        addCharacteristic(new HomekitCurrentAirPurifierStateCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        addCharacteristic(new HomekitTargetAirPurifierStateCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        addCharacteristic(new HomekitLockPhysicalControlsCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(false));
        addCharacteristic(new HomekitNameCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                .withMandatory(false));
        addCharacteristic(
                new HomekitRotationSpeedCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(false));
        addCharacteristic(
                new HomekitSwingModeCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(false));
        logger.debug("{}Added characteristics for Air Purifier service", LOG_CONFIG);
    }

    /**
     * Indicates whether this service can be extended with additional characteristics.
     *
     * @return false, as this service does not support extension
     * @since 1.0
     */
    @Override
    public boolean isExtensible() {
        return false;
    }
}
