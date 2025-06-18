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
import org.openhab.io.homekit.library.characteristic.HomekitCurrentDoorStateCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitNameCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitObstructionDetectedCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitStatusActiveCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitStatusFaultCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitTargetDoorStateCharacteristic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HomeKit Garage Door Opener Service.
 * <p>
 * This service provides control and monitoring of garage door openers in HomeKit accessories.
 * It enables door state control, obstruction detection, and status monitoring for garage doors.
 * </p>
 *
 * <ul>
 * <li>Door state control and monitoring</li>
 * <li>Obstruction detection</li>
 * <li>Status and fault monitoring</li>
 * </ul>
 *
 * <p>
 * Required characteristics:
 * <ul>
 * <li>CurrentDoorState - Current state of the garage door</li>
 * <li>TargetDoorState - Target state to move the door to</li>
 * <li>ObstructionDetected - Detection of door obstruction</li>
 * </ul>
 * </p>
 *
 * <p>
 * Optional characteristics:
 * <ul>
 * <li>Name - Door name</li>
 * <li>StatusActive - Door activation state</li>
 * <li>StatusFault - Fault state indicator</li>
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
@HomekitServiceType(type = "00000041-0000-1000-8000-0026BB765291", name = "Garage Door Opener", tag = "garageDoorOpener")
@NonNullByDefault
public class HomekitGarageDoorOpenerService extends AbstractHomekitService {
        // ========== Log Message Prefixes ==========
        private static final String LOG_PREFIX = "Homekit GarageDoorOpenerService: ";
        private static final String LOG_INIT = LOG_PREFIX + "Init - ";
        private static final String LOG_STATE = LOG_PREFIX + "State - ";
        private static final String LOG_TRACE = LOG_PREFIX + "Trace - ";

        private static final Logger logger = LoggerFactory.getLogger(HomekitGarageDoorOpenerService.class);

        /**
         * Creates a new Garage Door Opener service.
         *
         * @param accessory The accessory this service belongs to
         * @param eventManager The event manager for handling HomeKit events
         * @param characteristicFactory Factory for creating HomeKit characteristics
         * @since 1.0
         */
        public HomekitGarageDoorOpenerService(HomekitAccessory accessory, HomekitEventManager eventManager,
                        HomekitCharacteristicFactory characteristicFactory) {
                super(accessory, eventManager, characteristicFactory);
                withName("Garage Door Opener").withPrimary(false).withHidden(false).withExtensible(false);
                logger.debug("{}Created GarageDoorOpenerService for accessory {}", LOG_INIT, accessory.getLabel());
        }

        /**
         * Creates a new Garage Door Opener service from a JSON configuration.
         *
         * @param accessory The accessory this service belongs to
         * @param eventManager The event manager for handling HomeKit events
         * @param characteristicFactory Factory for creating HomeKit characteristics
         * @param value JSON value containing service configuration
         * @since 1.0
         */
        public HomekitGarageDoorOpenerService(HomekitAccessory accessory, HomekitEventManager eventManager,
                        HomekitCharacteristicFactory characteristicFactory, JsonValue value) {
                super(accessory, eventManager, characteristicFactory, value);
                logger.debug("{}Created GarageDoorOpenerService from JSON for accessory {}", LOG_INIT,
                                accessory.getLabel());
        }

        /**
         * Adds the required and optional characteristics for this service.
         * <p>
         * Required characteristics:
         * <ul>
         * <li>CurrentDoorState - Current state of the garage door</li>
         * <li>TargetDoorState - Target state to move the door to</li>
         * <li>ObstructionDetected - Detection of door obstruction</li>
         * </ul>
         * </p>
         * <p>
         * Optional characteristics:
         * <ul>
         * <li>Name - Door name</li>
         * <li>StatusActive - Door activation state</li>
         * <li>StatusFault - Fault state indicator</li>
         * </ul>
         * </p>
         * 
         * @since 1.0
         */
        @Override
        public void addCharacteristics() throws HomekitServiceException {
                logger.debug("{}Adding required characteristics to GarageDoorOpenerService for accessory {}", LOG_TRACE,
                                getAccessory().getLabel());

                addCharacteristic(
                                new HomekitCurrentDoorStateCharacteristic(this, eventManager,
                                                getAccessory().getNextAvailableInstanceId())
                                                .withMandatory(true));
                logger.debug("{}Added CurrentDoorStateCharacteristic to GarageDoorOpenerService", LOG_STATE);

                addCharacteristic(
                                new HomekitTargetDoorStateCharacteristic(this, eventManager,
                                                getAccessory().getNextAvailableInstanceId())
                                                .withMandatory(true));
                logger.debug("{}Added TargetDoorStateCharacteristic to GarageDoorOpenerService", LOG_STATE);

                addCharacteristic(
                                new HomekitObstructionDetectedCharacteristic(this, eventManager,
                                                getAccessory().getNextAvailableInstanceId())
                                                .withMandatory(true));
                logger.debug("{}Added ObstructionDetectedCharacteristic to GarageDoorOpenerService", LOG_STATE);

                addCharacteristic(
                                new HomekitNameCharacteristic(this, eventManager,
                                                getAccessory().getNextAvailableInstanceId())
                                                .withMandatory(false));
                logger.debug("{}Added NameCharacteristic to GarageDoorOpenerService", LOG_STATE);

                addCharacteristic(
                                new HomekitStatusActiveCharacteristic(this, eventManager,
                                                getAccessory().getNextAvailableInstanceId())
                                                .withMandatory(false));
                logger.debug("{}Added StatusActiveCharacteristic to GarageDoorOpenerService", LOG_STATE);

                addCharacteristic(
                                new HomekitStatusFaultCharacteristic(this, eventManager,
                                                getAccessory().getNextAvailableInstanceId())
                                                .withMandatory(false));
                logger.debug("{}Added StatusFaultCharacteristic to GarageDoorOpenerService", LOG_STATE);
        }
}
