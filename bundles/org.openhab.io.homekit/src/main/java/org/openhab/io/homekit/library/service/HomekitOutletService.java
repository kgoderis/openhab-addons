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
import org.openhab.io.homekit.library.characteristic.HomekitInUseCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitNameCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitOnCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitStatusActiveCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitStatusFaultCharacteristic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HomeKit Outlet Service.
 * <p>
 * This service provides control and monitoring of power outlets in HomeKit accessories.
 * It enables tracking of power state, usage status, and fault conditions.
 * </p>
 *
 * <ul>
 * <li>Power state control and monitoring</li>
 * <li>Usage status tracking</li>
 * <li>Fault condition monitoring</li>
 * <li>Active state tracking</li>
 * </ul>
 *
 * <p>
 * Required characteristics:
 * <ul>
 * <li>On - Current power state (0 = Off, 1 = On)</li>
 * <li>InUse - Current usage state (0 = Not In Use, 1 = In Use)</li>
 * </ul>
 * </p>
 *
 * <p>
 * Optional characteristics:
 * <ul>
 * <li>Name - Outlet name</li>
 * <li>StatusActive - Outlet activation state</li>
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
@HomekitServiceType(type = "00000047-0000-1000-8000-0026BB765291", name = "Outlet", tag = "outlet")
@NonNullByDefault
public class HomekitOutletService extends AbstractHomekitService {
    // ========== Log Message Prefixes ==========
    private static final String LOG_PREFIX = "Homekit OutletService: ";
    private static final String LOG_INIT = LOG_PREFIX + "Init - ";
    private static final String LOG_STATE = LOG_PREFIX + "State - ";
    private static final String LOG_TRACE = LOG_PREFIX + "Trace - ";

    private final Logger logger = LoggerFactory.getLogger(HomekitOutletService.class);

    /**
     * Creates a new Outlet service.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @since 1.0
     */
    public HomekitOutletService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory) {
        super(accessory, eventManager, characteristicFactory);
        withName("Outlet").withPrimary(false).withHidden(false);
        logger.debug("{}Created OutletService for accessory {}", LOG_INIT, accessory.getLabel());
    }

    /**
     * Creates a new Outlet service from a JSON configuration.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @param value JSON value containing service configuration
     * @since 1.0
     */
    public HomekitOutletService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory, JsonValue value) {
        super(accessory, eventManager, characteristicFactory, value);
        logger.debug("{}Created OutletService from JSON for accessory {}", LOG_INIT, accessory.getLabel());
    }

    /**
     * Adds the required and optional characteristics for this service.
     * <p>
     * Required characteristics:
     * <ul>
     * <li>On - Current power state (0 = Off, 1 = On)</li>
     * <li>InUse - Current usage state (0 = Not In Use, 1 = In Use)</li>
     * </ul>
     * </p>
     * <p>
     * Optional characteristics:
     * <ul>
     * <li>Name - Outlet name</li>
     * <li>StatusActive - Outlet activation state</li>
     * <li>StatusFault - Fault state indicator</li>
     * </ul>
     * </p>
     * 
     * @since 1.0
     */
    @Override
    public void addCharacteristics() throws HomekitServiceException {
        logger.debug("{}Adding required characteristics to OutletService for accessory {}", LOG_TRACE,
                getAccessory().getLabel());

        addCharacteristic(new HomekitOnCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                .withMandatory(true));
        logger.debug("{}Added OnCharacteristic to OutletService", LOG_STATE);

        addCharacteristic(
                new HomekitInUseCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(true));
        logger.debug("{}Added InUseCharacteristic to OutletService", LOG_STATE);

        addCharacteristic(new HomekitNameCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                .withMandatory(false));
        logger.debug("{}Added NameCharacteristic to OutletService", LOG_STATE);

        addCharacteristic(
                new HomekitStatusActiveCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(false));
        logger.debug("{}Added StatusActiveCharacteristic to OutletService", LOG_STATE);

        addCharacteristic(
                new HomekitStatusFaultCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(false));
        logger.debug("{}Added StatusFaultCharacteristic to OutletService", LOG_STATE);
    }

    /**
     * Indicates that this service is not extensible.
     * Outlet service has a fixed set of characteristics.
     *
     * @return false, as this service is not extensible
     * @since 1.0
     */
    @Override
    public boolean isExtensible() {
        return false;
    }
}
