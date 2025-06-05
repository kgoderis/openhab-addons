package org.openhab.io.homekit.library.service;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.service.HomekitServiceType;
import org.openhab.io.homekit.core.service.AbstractHomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.exception.HomekitServiceException;
import org.openhab.io.homekit.library.characteristic.HomekitNameCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitSmokeDetectedCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitStatusActiveCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitStatusFaultCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitStatusLowBatteryCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitStatusTamperedCharacteristic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HomeKit Smoke Sensor Service.
 * <p>
 * This service provides monitoring of smoke detection in HomeKit accessories.
 * It enables tracking of smoke detection state, battery level, tamper detection, and fault conditions.
 * </p>
 *
 * <ul>
 * <li>Smoke detection monitoring</li>
 * <li>Battery level monitoring</li>
 * <li>Tamper detection</li>
 * <li>Fault condition monitoring</li>
 * <li>Active state tracking</li>
 * </ul>
 *
 * <p>
 * Required characteristics:
 * <ul>
 * <li>SmokeDetected - Current smoke detection state (0 = Smoke Not Detected, 1 = Smoke Detected)</li>
 * </ul>
 * </p>
 *
 * <p>
 * Optional characteristics:
 * <ul>
 * <li>Name - Smoke sensor name</li>
 * <li>StatusActive - Sensor activation state</li>
 * <li>StatusFault - Fault state indicator</li>
 * <li>StatusLowBattery - Low battery indicator</li>
 * <li>StatusTampered - Tamper detection indicator</li>
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
@HomekitServiceType(type = "00000087-0000-1000-8000-0026BB765291", name = "Smoke Sensor", tag = "smokeSensor")
@NonNullByDefault
public class HomekitSmokeSensorService extends AbstractHomekitService {
    // ========== Log Message Prefixes ==========
    private static final String LOG_PREFIX = "Homekit SmokeSensorService: ";
    private static final String LOG_INIT = LOG_PREFIX + "Init - ";
    private static final String LOG_STATE = LOG_PREFIX + "State - ";
    private static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    private static final String LOG_ACCESSORY = LOG_PREFIX + "HomekitAccessory - ";
    private static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    private static final String LOG_WARN = LOG_PREFIX + "Warning - ";
    private static final String LOG_TRACE = LOG_PREFIX + "Trace - ";

    private final Logger logger = LoggerFactory.getLogger(HomekitSmokeSensorService.class);

    /**
     * Creates a new Smoke Sensor service.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @since 1.0
     */
    public HomekitSmokeSensorService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory) {
        super(accessory, eventManager, characteristicFactory);
        withName("Smoke Sensor").withPrimary(false).withHidden(false);
        logger.debug("{}Created SmokeSensorService for accessory {}", LOG_INIT, accessory.getLabel());
    }

    /**
     * Creates a new Smoke Sensor service from a JSON configuration.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @param value JSON value containing service configuration
     * @since 1.0
     */
    public HomekitSmokeSensorService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory, JsonValue value) {
        super(accessory, eventManager, characteristicFactory, value);
        logger.debug("{}Created SmokeSensorService from JSON for accessory {}", LOG_INIT, accessory.getLabel());
    }

    /**
     * Adds the required and optional characteristics for this service.
     * <p>
     * Required characteristics:
     * <ul>
     * <li>SmokeDetected - Current smoke detection state (0 = Smoke Not Detected, 1 = Smoke Detected)</li>
     * </ul>
     * </p>
     * <p>
     * Optional characteristics:
     * <ul>
     * <li>Name - Smoke sensor name</li>
     * <li>StatusActive - Sensor activation state</li>
     * <li>StatusFault - Fault state indicator</li>
     * <li>StatusLowBattery - Low battery indicator</li>
     * <li>StatusTampered - Tamper detection indicator</li>
     * </ul>
     * </p>
     * 
     * @since 1.0
     */
    @Override
    public void addCharacteristics() throws HomekitServiceException {
        logger.trace("{}Adding required characteristics to SmokeSensorService for accessory {}", LOG_TRACE,
                getAccessory().getLabel());

        addCharacteristic(
                new HomekitSmokeDetectedCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(true));
        logger.debug("{}Added SmokeDetectedCharacteristic to SmokeSensorService", LOG_STATE);

        addCharacteristic(new HomekitNameCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                .withMandatory(false));
        logger.debug("{}Added NameCharacteristic to SmokeSensorService", LOG_STATE);

        addCharacteristic(
                new HomekitStatusActiveCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(false));
        logger.debug("{}Added StatusActiveCharacteristic to SmokeSensorService", LOG_STATE);

        addCharacteristic(
                new HomekitStatusFaultCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(false));
        logger.debug("{}Added StatusFaultCharacteristic to SmokeSensorService", LOG_STATE);

        addCharacteristic(new HomekitStatusLowBatteryCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(false));
        logger.debug("{}Added StatusLowBatteryCharacteristic to SmokeSensorService", LOG_STATE);

        addCharacteristic(
                new HomekitStatusTamperedCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(false));
        logger.debug("{}Added StatusTamperedCharacteristic to SmokeSensorService", LOG_STATE);
    }

    /**
     * Indicates that this service is not extensible.
     * Smoke Sensor service has a fixed set of characteristics.
     *
     * @return false, as this service is not extensible
     * @since 1.0
     */
    @Override
    public boolean isExtensible() {
        return false;
    }
}
