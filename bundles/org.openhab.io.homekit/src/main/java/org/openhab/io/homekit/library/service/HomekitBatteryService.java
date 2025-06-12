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
import org.openhab.io.homekit.library.characteristic.HomekitBatteryLevelCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitChargingStateCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitStatusLowBatteryCharacteristic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HomeKit Battery Service.
 * <p>
 * This service provides comprehensive battery information and status monitoring for HomeKit accessories.
 * It enables tracking of battery levels, charging states, and low battery warnings for battery-powered devices.
 * </p>
 *
 * <ul>
 * <li>Battery level monitoring and reporting</li>
 * <li>Charging state detection and notification</li>
 * <li>Low battery status alerts</li>
 * </ul>
 *
 * <p>
 * Required characteristics:
 * <ul>
 * <li>BatteryLevel - Current battery charge percentage</li>
 * <li>ChargingState - Current charging status (charging/not charging)</li>
 * <li>StatusLowBattery - Low battery warning indicator</li>
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
@HomekitServiceType(type = "00000096-0000-1000-8000-0026BB765291", name = "Battery", tag = "battery")
@NonNullByDefault
public class HomekitBatteryService extends AbstractHomekitService {
    // ========== Log Message Prefixes ==========
    private static final String LOG_PREFIX = "Homekit BatteryService: ";
    private static final String LOG_INIT = LOG_PREFIX + "Init - ";
    private static final String LOG_STATE = LOG_PREFIX + "State - ";
    private static final String LOG_TRACE = LOG_PREFIX + "Trace - ";

    private final Logger logger = LoggerFactory.getLogger(HomekitBatteryService.class);

    /**
     * Creates a new Battery service.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @since 1.0
     */
    public HomekitBatteryService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory) {
        super(accessory, eventManager, characteristicFactory);
        withName("Battery").withPrimary(false).withHidden(false);
        logger.debug("{}Created BatteryService for accessory {}", LOG_INIT, accessory.getLabel());
    }

    /**
     * Creates a new Battery service from a JSON configuration.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @param value JSON value containing service configuration
     * @since 1.0
     */
    public HomekitBatteryService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory, JsonValue value) {
        super(accessory, eventManager, characteristicFactory, value);
        logger.debug("{}Created BatteryService from JSON for accessory {}", LOG_INIT, accessory.getLabel());
    }

    /**
     * Adds the required characteristics for this service.
     * <p>
     * Required characteristics:
     * <ul>
     * <li>BatteryLevel - Current battery charge percentage</li>
     * <li>ChargingState - Current charging status</li>
     * <li>StatusLowBattery - Low battery warning indicator</li>
     * </ul>
     * </p>
     * 
     * @since 1.0
     */
    @Override
    public void addCharacteristics() throws HomekitServiceException {
        logger.trace("{}Adding required characteristics to BatteryService for accessory {}", LOG_TRACE,
                getAccessory().getLabel());

        addCharacteristic(
                new HomekitBatteryLevelCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(true));
        logger.debug("{}Added BatteryLevelCharacteristic to BatteryService", LOG_STATE);

        addCharacteristic(
                new HomekitChargingStateCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(true));
        logger.debug("{}Added ChargingStateCharacteristic to BatteryService", LOG_STATE);

        addCharacteristic(new HomekitStatusLowBatteryCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        logger.debug("{}Added StatusLowBatteryCharacteristic to BatteryService", LOG_STATE);
    }
}
