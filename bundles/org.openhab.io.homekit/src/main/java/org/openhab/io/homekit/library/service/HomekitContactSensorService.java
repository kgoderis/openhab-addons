/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.openhab.io.homekit.library.service;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.service.HomekitServiceType;
import org.openhab.io.homekit.core.service.AbstractHomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.exception.HomekitServiceException;
import org.openhab.io.homekit.library.characteristic.HomekitContactSensorStateCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitNameCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitStatusActiveCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitStatusFaultCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitStatusLowBatteryCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitStatusTamperedCharacteristic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HomeKit Contact Sensor Service.
 * <p>
 * This service provides monitoring of contact sensor states in HomeKit accessories.
 * It enables tracking of door/window open/closed states and sensor status information.
 * </p>
 *
 * <ul>
 * <li>Contact state monitoring (open/closed)</li>
 * <li>Sensor status and fault monitoring</li>
 * <li>Battery and tamper state tracking</li>
 * </ul>
 *
 * <p>
 * Required characteristics:
 * <ul>
 * <li>ContactSensorState - Current contact state (open/closed)</li>
 * </ul>
 * </p>
 *
 * <p>
 * Optional characteristics:
 * <ul>
 * <li>Name - Sensor name</li>
 * <li>StatusActive - Sensor activation state</li>
 * <li>StatusFault - Fault state indicator</li>
 * <li>StatusLowBattery - Low battery warning</li>
 * <li>StatusTampered - Tamper detection state</li>
 * </ul>
 * </p>
 *
 * <p>
 * For more information, see the
 * <a href="https://developer.apple.com/documentation/HomeKit">HomeKit Accessory Protocol Specification</a>.
 * </p>
 *
 * @author Karel Goderis - Initial Contribution
 * @version 1.0
 * @since 1.0
 */
@HomekitServiceType(type = "00000080-0000-1000-8000-0026BB765291", name = "Contact Sensor", tag = "contactSensor")
@NonNullByDefault
public class HomekitContactSensorService extends AbstractHomekitService {
        // ========== Log Message Prefixes ==========
        private static final String LOG_PREFIX = "Homekit ContactSensorService: ";
        private static final String LOG_INIT = LOG_PREFIX + "Init - ";
        private static final String LOG_STATE = LOG_PREFIX + "State - ";
        private static final String LOG_TRACE = LOG_PREFIX + "Trace - ";

        private static final Logger logger = LoggerFactory.getLogger(HomekitContactSensorService.class);

        /**
         * Creates a new Contact Sensor service.
         *
         * @param accessory The accessory this service belongs to
         * @param eventManager The event manager for handling HomeKit events
         * @param characteristicFactory Factory for creating HomeKit characteristics
         * @since 1.0
         */
        public HomekitContactSensorService(HomekitAccessory accessory, HomekitEventManager eventManager,
                        HomekitCharacteristicFactory characteristicFactory) {
                super(accessory, eventManager, characteristicFactory);
                withName("Contact Sensor").withPrimary(false).withHidden(false).withExtensible(false);
                logger.debug("{}Created ContactSensorService for accessory {}", LOG_INIT, accessory.getLabel());
        }

        /**
         * Creates a new Contact Sensor service from a JSON configuration.
         *
         * @param accessory The accessory this service belongs to
         * @param eventManager The event manager for handling HomeKit events
         * @param characteristicFactory Factory for creating HomeKit characteristics
         * @param value JSON value containing service configuration
         * @since 1.0
         */
        public HomekitContactSensorService(HomekitAccessory accessory, HomekitEventManager eventManager,
                        HomekitCharacteristicFactory characteristicFactory, JsonValue value) {
                super(accessory, eventManager, characteristicFactory, value);
                logger.debug("{}Created ContactSensorService from JSON for accessory {}", LOG_INIT,
                                accessory.getLabel());
        }

        /**
         * Adds the required and optional characteristics for this service.
         * <p>
         * Required characteristics:
         * <ul>
         * <li>ContactSensorState - Current contact state (open/closed)</li>
         * </ul>
         * </p>
         * <p>
         * Optional characteristics:
         * <ul>
         * <li>Name - Sensor name</li>
         * <li>StatusActive - Sensor activation state</li>
         * <li>StatusFault - Fault state indicator</li>
         * <li>StatusLowBattery - Low battery warning</li>
         * <li>StatusTampered - Tamper detection state</li>
         * </ul>
         * </p>
         * 
         * @since 1.0
         */
        @Override
        public void addCharacteristics() throws HomekitServiceException {
                logger.debug("{}Adding required characteristics to ContactSensorService for accessory {}", LOG_TRACE,
                                getAccessory().getLabel());

                addCharacteristic(new HomekitContactSensorStateCharacteristic(this, eventManager,
                                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
                logger.debug("{}Added ContactSensorStateCharacteristic to ContactSensorService", LOG_STATE);

                addCharacteristic(new HomekitNameCharacteristic(this, eventManager,
                                getAccessory().getNextAvailableInstanceId())
                                .withMandatory(false));
                logger.debug("{}Added NameCharacteristic to ContactSensorService", LOG_STATE);

                addCharacteristic(
                                new HomekitStatusActiveCharacteristic(this, eventManager,
                                                getAccessory().getNextAvailableInstanceId())
                                                .withMandatory(false));
                logger.debug("{}Added StatusActiveCharacteristic to ContactSensorService", LOG_STATE);

                addCharacteristic(
                                new HomekitStatusFaultCharacteristic(this, eventManager,
                                                getAccessory().getNextAvailableInstanceId())
                                                .withMandatory(false));
                logger.debug("{}Added StatusFaultCharacteristic to ContactSensorService", LOG_STATE);

                addCharacteristic(new HomekitStatusLowBatteryCharacteristic(this, eventManager,
                                getAccessory().getNextAvailableInstanceId()).withMandatory(false));
                logger.debug("{}Added StatusLowBatteryCharacteristic to ContactSensorService", LOG_STATE);

                addCharacteristic(
                                new HomekitStatusTamperedCharacteristic(this, eventManager,
                                                getAccessory().getNextAvailableInstanceId())
                                                .withMandatory(false));
                logger.debug("{}Added StatusTamperedCharacteristic to ContactSensorService", LOG_STATE);
        }
}
