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
import org.openhab.io.homekit.library.characteristic.HomekitActiveCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitInUseCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitRemainingDurationCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitSetDurationCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitValveTypeCharacteristic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HomeKit Valve Service.
 * <p>
 * This service provides control and monitoring of valves in HomeKit accessories.
 * Valves can be used to control water, gas, or other fluid flow in a home automation system.
 * </p>
 *
 * <ul>
 * <li>Valve state control (active/inactive)</li>
 * <li>Usage status monitoring</li>
 * <li>Duration control and monitoring</li>
 * <li>Valve type configuration</li>
 * </ul>
 *
 * <p>
 * Required characteristics:
 * <ul>
 * <li>Active - Current valve state (0 = Inactive, 1 = Active)</li>
 * <li>InUse - Current usage state (0 = Not in use, 1 = In use)</li>
 * <li>ValveType - Type of valve (0 = Generic, 1 = Irrigation, 2 = Shower, 3 = Water faucet)</li>
 * </ul>
 * </p>
 *
 * <p>
 * Optional characteristics:
 * <ul>
 * <li>Name - Valve name</li>
 * <li>SetDuration - Duration setting in seconds</li>
 * <li>RemainingDuration - Remaining duration in seconds</li>
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
@HomekitServiceType(type = "000000D0-0000-1000-8000-0026BB765291", name = "Valve", tag = "valve")
@NonNullByDefault
public class HomekitValveService extends AbstractHomekitService {
    // ========== Log Message Prefixes ==========
    private static final String LOG_PREFIX = "Homekit ValveService: ";
    private static final String LOG_INIT = LOG_PREFIX + "Init - ";
    private static final String LOG_TRACE = LOG_PREFIX + "Trace - ";
    private static final String LOG_CONFIG = LOG_PREFIX + "Config - ";

    private static final Logger logger = LoggerFactory.getLogger(HomekitValveService.class);

    /**
     * Creates a new Valve service.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @since 1.0
     */
    public HomekitValveService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory) {
        super(accessory, eventManager, characteristicFactory);
        withName("Valve").withPrimary(false).withHidden(false).withExtensible(false);
        logger.debug("{}Created ValveService for accessory {}", LOG_INIT, accessory.getLabel());
    }

    /**
     * Creates a new Valve service from a JSON configuration.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @param value JSON value containing service configuration
     * @since 1.0
     */
    public HomekitValveService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory, JsonValue value) {
        super(accessory, eventManager, characteristicFactory, value);
        logger.debug("{}Created ValveService from JSON configuration for accessory {}", LOG_INIT, accessory.getLabel());
    }

    /**
     * Adds the required and optional characteristics for this service.
     * 
     * <p>
     * Required characteristics:
     * <ul>
     * <li>Active (UUID: 000000B0-0000-1000-8000-0026BB765291)</li>
     * <li>InUse (UUID: 000000D2-0000-1000-8000-0026BB765291)</li>
     * <li>ValveType (UUID: 000000D5-0000-1000-8000-0026BB765291)</li>
     * </ul>
     * </p>
     *
     * <p>
     * Optional characteristics:
     * <ul>
     * <li>Name (UUID: 00000023-0000-1000-8000-0026BB765291)</li>
     * <li>SetDuration (UUID: 000000D3-0000-1000-8000-0026BB765291)</li>
     * <li>RemainingDuration (UUID: 000000D4-0000-1000-8000-0026BB765291)</li>
     * </ul>
     * </p>
     *
     * @since 1.0
     */
    @Override
    public void addCharacteristics() throws HomekitServiceException {
        // Required characteristics
        addCharacteristic(
                new HomekitActiveCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(true));
        addCharacteristic(
                new HomekitInUseCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(true));
        addCharacteristic(
                new HomekitValveTypeCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(true));
        logger.debug("{}Added required characteristics: Active, InUse, ValveType", LOG_TRACE);

        // Optional characteristics
        addCharacteristic(
                new HomekitSetDurationCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId()));
        addCharacteristic(new HomekitRemainingDurationCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()));
        logger.debug("{}Added optional characteristics: Name, SetDuration, RemainingDuration", LOG_TRACE);

        logger.debug("{}Added characteristics for Valve service", LOG_CONFIG);
    }
}
