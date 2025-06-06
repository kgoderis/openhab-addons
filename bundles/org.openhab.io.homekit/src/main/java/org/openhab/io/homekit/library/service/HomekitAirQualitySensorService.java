package org.openhab.io.homekit.library.service;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.service.HomekitServiceType;
import org.openhab.io.homekit.core.service.AbstractHomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.exception.HomekitServiceException;
import org.openhab.io.homekit.library.characteristic.HomekitAirParticulateDensityCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitAirParticulateSizeCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitAirQualityCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitNitrogenDioxideDensityCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitOzoneDensityCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitPM10DensityCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitSulphurDioxideDensityCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitVOCDensityCharacteristic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HomeKit Air Quality Sensor Service.
 * 
 * <p>
 * This service provides information about air quality and various air pollutants, including:
 * <ul>
 * <li>Overall air quality measurement</li>
 * <li>Particulate matter density</li>
 * <li>Chemical pollutant levels</li>
 * <li>VOC (Volatile Organic Compounds) monitoring</li>
 * </ul>
 * </p>
 *
 * <p>
 * The service is used to:
 * <ul>
 * <li>Monitor overall air quality</li>
 * <li>Track specific pollutant levels</li>
 * <li>Measure particulate matter</li>
 * <li>Detect harmful gases</li>
 * <li>Monitor VOC concentrations</li>
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
@HomekitServiceType(type = "0000008D-0000-1000-8000-0026BB765291", name = "Air Quality Sensor", tag = "airQualitySensor")
@NonNullByDefault
public class HomekitAirQualitySensorService extends AbstractHomekitService {
        // ========== Log Message Prefixes ==========
        private static final String LOG_PREFIX = "Homekit AirQualitySensorService: ";
        private static final String LOG_INIT = LOG_PREFIX + "Init - ";
        private static final String LOG_CONFIG = LOG_PREFIX + "Config - ";

        private final Logger logger = LoggerFactory.getLogger(HomekitAirQualitySensorService.class);

        /**
         * Creates a new Air Quality Sensor service.
         *
         * @param accessory The accessory this service belongs to
         * @param eventManager The event manager for handling HomeKit events
         * @param characteristicFactory Factory for creating HomeKit characteristics
         * @since 1.0
         */
        public HomekitAirQualitySensorService(HomekitAccessory accessory, HomekitEventManager eventManager,
                        HomekitCharacteristicFactory characteristicFactory) {
                super(accessory, eventManager, characteristicFactory);
                withName("Air Quality Sensor").withPrimary(false).withHidden(false);
                logger.debug("{}Created new Air Quality Sensor service for accessory {}", LOG_INIT,
                                accessory.getLabel());
        }

        /**
         * Creates a new Air Quality Sensor service from a JSON configuration.
         *
         * @param accessory The accessory this service belongs to
         * @param eventManager The event manager for handling HomeKit events
         * @param characteristicFactory Factory for creating HomeKit characteristics
         * @param value JSON value containing service configuration
         * @since 1.0
         */
        public HomekitAirQualitySensorService(HomekitAccessory accessory, HomekitEventManager eventManager,
                        HomekitCharacteristicFactory characteristicFactory, JsonValue value) {
                super(accessory, eventManager, characteristicFactory, value);
                logger.debug("{}Created new Air Quality Sensor service from JSON configuration for accessory {}",
                                LOG_INIT,
                                accessory.getLabel());
        }

        /**
         * Adds the required and optional characteristics for this service.
         * 
         * <p>
         * Required characteristics:
         * <ul>
         * <li>AirQuality (UUID: 00000095-0000-1000-8000-0026BB765291)</li>
         * </ul>
         * </p>
         *
         * <p>
         * Optional characteristics:
         * <ul>
         * <li>AirParticulateDensity (UUID: 00000064-0000-1000-8000-0026BB765291)</li>
         * <li>AirParticulateSize (UUID: 00000065-0000-1000-8000-0026BB765291)</li>
         * <li>OzoneDensity (UUID: 000000C3-0000-1000-8000-0026BB765291)</li>
         * <li>NitrogenDioxideDensity (UUID: 000000C4-0000-1000-8000-0026BB765291)</li>
         * <li>SulphurDioxideDensity (UUID: 000000C5-0000-1000-8000-0026BB765291)</li>
         * <li>PM10Density (UUID: 000000C7-0000-1000-8000-0026BB765291)</li>
         * <li>VOCDensity (UUID: 000000C8-0000-1000-8000-0026BB765291)</li>
         * </ul>
         * </p>
         *
         * @since 1.0
         */
        @Override
        public void addCharacteristics() throws HomekitServiceException {
                addCharacteristic(
                                new HomekitAirQualityCharacteristic(this, eventManager,
                                                getAccessory().getNextAvailableInstanceId())
                                                .withMandatory(true));
                addCharacteristic(new HomekitAirParticulateDensityCharacteristic(this, eventManager,
                                getAccessory().getNextAvailableInstanceId()).withMandatory(false));
                addCharacteristic(new HomekitAirParticulateSizeCharacteristic(this, eventManager,
                                getAccessory().getNextAvailableInstanceId()).withMandatory(false));
                addCharacteristic(
                                new HomekitOzoneDensityCharacteristic(this, eventManager,
                                                getAccessory().getNextAvailableInstanceId())
                                                .withMandatory(false));
                addCharacteristic(new HomekitNitrogenDioxideDensityCharacteristic(this, eventManager,
                                getAccessory().getNextAvailableInstanceId()).withMandatory(false));
                addCharacteristic(new HomekitSulphurDioxideDensityCharacteristic(this, eventManager,
                                getAccessory().getNextAvailableInstanceId()).withMandatory(false));
                addCharacteristic(
                                new HomekitPM10DensityCharacteristic(this, eventManager,
                                                getAccessory().getNextAvailableInstanceId())
                                                .withMandatory(false));
                addCharacteristic(
                                new HomekitVOCDensityCharacteristic(this, eventManager,
                                                getAccessory().getNextAvailableInstanceId())
                                                .withMandatory(false));
                logger.debug("{}Added characteristics for Air Quality Sensor service", LOG_CONFIG);
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
