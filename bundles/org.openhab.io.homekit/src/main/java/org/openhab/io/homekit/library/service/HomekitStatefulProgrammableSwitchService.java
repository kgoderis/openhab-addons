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
import org.openhab.io.homekit.library.characteristic.HomekitNameCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitProgrammableSwitchEventCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitStatusActiveCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitStatusFaultCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitStatusLowBatteryCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitStatusTamperedCharacteristic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HomeKit Stateful Programmable Switch Service.
 * <p>
 * This service provides control and monitoring of stateful programmable switches in HomeKit accessories.
 * Stateful switches maintain their state after being triggered and can be used for toggle or latching functionality.
 * </p>
 *
 * <ul>
 * <li>State-based switch control</li>
 * <li>Status monitoring (active, fault, low battery, tampered)</li>
 * <li>State maintenance after triggering</li>
 * </ul>
 *
 * <p>
 * Required characteristics:
 * <ul>
 * <li>ProgrammableSwitchEvent - The type of event triggered by the switch</li>
 * </ul>
 * </p>
 *
 * <p>
 * Optional characteristics:
 * <ul>
 * <li>Name - Switch name</li>
 * <li>StatusActive - Switch activation state</li>
 * <li>StatusFault - Fault state indicator</li>
 * <li>StatusLowBattery - Low battery indicator</li>
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
@HomekitServiceType(type = "00000088-0000-1000-8000-0026BB765291", name = "Stateful Programmable Switch", tag = "statefulProgrammableSwitch")
@NonNullByDefault
public class HomekitStatefulProgrammableSwitchService extends AbstractHomekitService {
    // ========== Log Message Prefixes ==========
    private static final String LOG_PREFIX = "Homekit StatefulProgrammableSwitchService: ";
    private static final String LOG_INIT = LOG_PREFIX + "Init - ";
    private static final String LOG_TRACE = LOG_PREFIX + "Trace - ";

    private static final Logger logger = LoggerFactory.getLogger(HomekitStatefulProgrammableSwitchService.class);

    /**
     * Creates a new Stateful Programmable Switch service.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @since 1.0
     */
    public HomekitStatefulProgrammableSwitchService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory) {
        super(accessory, eventManager, characteristicFactory);
        withName("Stateful Programmable Switch").withPrimary(false).withHidden(false);
        logger.debug("{}Created StatefulProgrammableSwitchService for accessory {}", LOG_INIT, accessory.getLabel());
    }

    /**
     * Creates a new Stateful Programmable Switch service from a JSON configuration.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @param value JSON value containing service configuration
     * @since 1.0
     */
    public HomekitStatefulProgrammableSwitchService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory, JsonValue value) {
        super(accessory, eventManager, characteristicFactory, value);
        logger.debug("{}Created StatefulProgrammableSwitchService from JSON configuration for accessory {}", LOG_INIT,
                accessory.getLabel());
    }

    /**
     * Adds the required and optional characteristics for this service.
     * 
     * <p>
     * Required characteristics:
     * <ul>
     * <li>ProgrammableSwitchEvent (UUID: 00000073-0000-1000-8000-0026BB765291)</li>
     * </ul>
     * </p>
     *
     * <p>
     * Optional characteristics:
     * <ul>
     * <li>Name (UUID: 00000023-0000-1000-8000-0026BB765291)</li>
     * <li>StatusActive (UUID: 00000075-0000-1000-8000-0026BB765291)</li>
     * <li>StatusFault (UUID: 00000077-0000-1000-8000-0026BB765291)</li>
     * <li>StatusLowBattery (UUID: 00000079-0000-1000-8000-0026BB765291)</li>
     * <li>StatusTampered (UUID: 0000007A-0000-1000-8000-0026BB765291)</li>
     * </ul>
     * </p>
     *
     * @since 1.0
     */
    @Override
    public void addCharacteristics() throws HomekitServiceException {
        // Required characteristics
        addCharacteristic(new HomekitProgrammableSwitchEventCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        logger.debug("{}Added required ProgrammableSwitchEvent characteristic", LOG_TRACE);

        // Optional characteristics
        addCharacteristic(
                new HomekitNameCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId()));
        addCharacteristic(
                new HomekitStatusActiveCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId()));
        addCharacteristic(
                new HomekitStatusFaultCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId()));
        addCharacteristic(new HomekitStatusLowBatteryCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()));
        addCharacteristic(new HomekitStatusTamperedCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()));
        logger.debug(
                "{}Added optional characteristics: Name, StatusActive, StatusFault, StatusLowBattery, StatusTampered",
                LOG_TRACE);
    }

    @Override
    public boolean isExtensible() {
        return false;
    }
}
