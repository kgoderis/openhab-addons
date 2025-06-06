package org.openhab.io.homekit.library.service;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.service.HomekitServiceType;
import org.openhab.io.homekit.core.service.AbstractHomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.exception.HomekitServiceException;
import org.openhab.io.homekit.library.characteristic.HomekitCurrentTemperatureCharacteristic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HomeKit Temperature Sensor Service.
 * <p>
 * This service provides monitoring of temperature readings in HomeKit accessories.
 * It enables tracking of current temperature values in Celsius.
 * </p>
 *
 * <ul>
 * <li>Temperature monitoring</li>
 * <li>Real-time temperature updates</li>
 * <li>Temperature value in Celsius</li>
 * </ul>
 *
 * <p>
 * Required characteristics:
 * <ul>
 * <li>CurrentTemperature - Current temperature value in Celsius</li>
 * </ul>
 * </p>
 *
 * <p>
 * For more information, see the
 * <a href="https://developer.apple.com/documentation/HomeKit">HomeKit Accessory Protocol Specification</a>.
 * </p>
 *
 * @author Karel Goderis
 * @version 1.0
 * @since 1.0
 */
@HomekitServiceType(type = "0000008A-0000-1000-8000-0026BB765291", name = "Temperature Sensor", tag = "temperatureSensor")
@NonNullByDefault
public class HomekitTemperatureSensorService extends AbstractHomekitService {
    // ========== Log Message Prefixes ==========
    private static final String LOG_PREFIX = "Homekit TemperatureSensorService: ";
    private static final String LOG_INIT = LOG_PREFIX + "Init - ";
    private static final String LOG_STATE = LOG_PREFIX + "State - ";
    private static final String LOG_TRACE = LOG_PREFIX + "Trace - ";

    private final Logger logger = LoggerFactory.getLogger(HomekitTemperatureSensorService.class);

    /**
     * Creates a new Temperature Sensor service.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @since 1.0
     */
    public HomekitTemperatureSensorService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory) {
        super(accessory, eventManager, characteristicFactory);
        withName("Temperature Sensor").withPrimary(false).withHidden(false);
        logger.debug("{}Created TemperatureSensorService for accessory {}", LOG_INIT, accessory.getLabel());
    }

    /**
     * Creates a new Temperature Sensor service from a JSON configuration.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @param value JSON value containing service configuration
     * @since 1.0
     */
    public HomekitTemperatureSensorService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory, JsonValue value) {
        super(accessory, eventManager, characteristicFactory, value);
        logger.debug("{}Created TemperatureSensorService from JSON for accessory {}", LOG_INIT, accessory.getLabel());
    }

    /**
     * Adds the required characteristics for this service.
     * <p>
     * Required characteristics:
     * <ul>
     * <li>CurrentTemperature - Current temperature value in Celsius</li>
     * </ul>
     * </p>
     * 
     * @since 1.0
     */
    @Override
    public void addCharacteristics() throws HomekitServiceException {
        logger.trace("{}Adding required characteristics to TemperatureSensorService for accessory {}", LOG_TRACE,
                getAccessory().getLabel());

        addCharacteristic(new HomekitCurrentTemperatureCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        logger.debug("{}Added CurrentTemperatureCharacteristic to TemperatureSensorService", LOG_STATE);
    }

    /**
     * Indicates that this service is not extensible.
     * Temperature Sensor service has a fixed set of characteristics.
     *
     * @return false, as this service is not extensible
     * @since 1.0
     */
    @Override
    public boolean isExtensible() {
        return false;
    }
}
