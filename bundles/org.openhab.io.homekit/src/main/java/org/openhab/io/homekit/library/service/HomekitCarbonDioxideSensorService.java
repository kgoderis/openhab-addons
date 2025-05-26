package org.openhab.io.homekit.library.service;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.service.HomekitServiceType;
import org.openhab.io.homekit.core.service.AbstractHomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.library.characteristic.HomekitCarbonDioxideDetectedCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitCarbonDioxideLevelCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitCarbonDioxidePeakLevelCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitNameCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitStatusActiveCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitStatusFaultCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitStatusLowBatteryCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitStatusTamperedCharacteristic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HomeKit Carbon Dioxide Sensor Service.
 * <p>
 * This service provides comprehensive monitoring and detection of carbon dioxide (CO2) levels in HomeKit accessories.
 * It enables tracking of CO2 concentrations, peak levels, and detection states for air quality monitoring devices.
 * </p>
 *
 * <ul>
 *   <li>CO2 level monitoring and reporting</li>
 *   <li>Peak level detection and tracking</li>
 *   <li>Sensor status and fault monitoring</li>
 *   <li>Battery and tamper state tracking</li>
 * </ul>
 *
 * <p>
 * Required characteristics:
 * <ul>
 *   <li>CarbonDioxideDetected - Current CO2 detection state</li>
 * </ul>
 * </p>
 *
 * <p>
 * Optional characteristics:
 * <ul>
 *   <li>CarbonDioxideLevel - Current CO2 concentration</li>
 *   <li>CarbonDioxidePeakLevel - Highest recorded CO2 level</li>
 *   <li>Name - Sensor name</li>
 *   <li>StatusActive - Sensor activation state</li>
 *   <li>StatusFault - Fault state indicator</li>
 *   <li>StatusLowBattery - Low battery warning</li>
 *   <li>StatusTampered - Tamper detection state</li>
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
@HomekitServiceType(type = "00000097-0000-1000-8000-0026BB765291", name = "Carbon Dioxide Sensor", tag = "carbonDioxideSensor")
@NonNullByDefault
public class HomekitCarbonDioxideSensorService extends AbstractHomekitService {
    // ========== Log Message Prefixes ==========
    private static final String LOG_PREFIX = "Homekit CarbonDioxideSensorService: ";
    private static final String LOG_INIT = LOG_PREFIX + "Init - ";
    private static final String LOG_STATE = LOG_PREFIX + "State - ";
    private static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    private static final String LOG_ACCESSORY = LOG_PREFIX + "HomekitAccessory - ";
    private static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    private static final String LOG_WARN = LOG_PREFIX + "Warning - ";
    private static final String LOG_TRACE = LOG_PREFIX + "Trace - ";

    private final Logger logger = LoggerFactory.getLogger(HomekitCarbonDioxideSensorService.class);

    /**
     * Creates a new Carbon Dioxide Sensor service.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @since 1.0
     */
    public HomekitCarbonDioxideSensorService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory) {
        super(accessory, eventManager, characteristicFactory);
        withName("Carbon Dioxide Sensor").withPrimary(false).withHidden(false);
        logger.debug("{}Created CarbonDioxideSensorService for accessory {}", LOG_INIT, accessory.getLabel());
    }

    /**
     * Creates a new Carbon Dioxide Sensor service from a JSON configuration.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @param value JSON value containing service configuration
     * @since 1.0
     */
    public HomekitCarbonDioxideSensorService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory, JsonValue value) {
        super(accessory, eventManager, characteristicFactory, value);
        logger.debug("{}Created CarbonDioxideSensorService from JSON for accessory {}", LOG_INIT, accessory.getLabel());
    }

    /**
     * Adds the required and optional characteristics for this service.
     * <p>
     * Required characteristics:
     * <ul>
     *   <li>CarbonDioxideDetected - Current CO2 detection state</li>
     * </ul>
     * </p>
     * <p>
     * Optional characteristics:
     * <ul>
     *   <li>CarbonDioxideLevel - Current CO2 concentration</li>
     *   <li>CarbonDioxidePeakLevel - Highest recorded CO2 level</li>
     *   <li>Name - Sensor name</li>
     *   <li>StatusActive - Sensor activation state</li>
     *   <li>StatusFault - Fault state indicator</li>
     *   <li>StatusLowBattery - Low battery warning</li>
     *   <li>StatusTampered - Tamper detection state</li>
     * </ul>
     * </p>
     * @since 1.0
     */
    @Override
    public void addCharacteristics() {
        logger.trace("{}Adding required characteristics to CarbonDioxideSensorService for accessory {}", LOG_TRACE, getAccessory().getLabel());
        
        addCharacteristic(new HomekitCarbonDioxideDetectedCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        logger.debug("{}Added CarbonDioxideDetectedCharacteristic to CarbonDioxideSensorService", LOG_STATE);
        
        addCharacteristic(new HomekitCarbonDioxideLevelCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(false));
        logger.debug("{}Added CarbonDioxideLevelCharacteristic to CarbonDioxideSensorService", LOG_STATE);
        
        addCharacteristic(new HomekitCarbonDioxidePeakLevelCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(false));
        logger.debug("{}Added CarbonDioxidePeakLevelCharacteristic to CarbonDioxideSensorService", LOG_STATE);
        
        addCharacteristic(new HomekitNameCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                .withMandatory(false));
        logger.debug("{}Added NameCharacteristic to CarbonDioxideSensorService", LOG_STATE);
        
        addCharacteristic(
                new HomekitStatusActiveCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(false));
        logger.debug("{}Added StatusActiveCharacteristic to CarbonDioxideSensorService", LOG_STATE);
        
        addCharacteristic(
                new HomekitStatusFaultCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(false));
        logger.debug("{}Added StatusFaultCharacteristic to CarbonDioxideSensorService", LOG_STATE);
        
        addCharacteristic(new HomekitStatusLowBatteryCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(false));
        logger.debug("{}Added StatusLowBatteryCharacteristic to CarbonDioxideSensorService", LOG_STATE);
        
        addCharacteristic(
                new HomekitStatusTamperedCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(false));
        logger.debug("{}Added StatusTamperedCharacteristic to CarbonDioxideSensorService", LOG_STATE);
    }

    /**
     * Indicates that this service is not extensible.
     * Carbon dioxide sensor service has a fixed set of characteristics.
     *
     * @return false, as this service is not extensible
     * @since 1.0
     */
    @Override
    public boolean isExtensible() {
        return false;
    }
}
