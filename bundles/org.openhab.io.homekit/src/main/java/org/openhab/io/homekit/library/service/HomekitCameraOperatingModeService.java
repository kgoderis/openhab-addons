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
import org.openhab.io.homekit.library.characteristic.HomekitCameraOperatingModeIndicatorCharacteristic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HomeKit Camera Operating Mode Service.
 * <p>
 * This service provides functionality for controlling and monitoring camera operating modes in HomeKit accessories.
 * It enables management of camera states, modes, and operational settings for IP cameras and other video devices.
 * </p>
 *
 * <ul>
 * <li>Camera operating mode control</li>
 * <li>Mode indicator status monitoring</li>
 * <li>Operational state management</li>
 * </ul>
 *
 * <p>
 * Required characteristics:
 * <ul>
 * <li>CameraOperatingModeIndicator - Current operating mode status</li>
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
@HomekitServiceType(type = "0000021A-0000-1000-8000-0026BB765291", name = "CameraOperatingMode", tag = "cameraOperatingMode")
@NonNullByDefault
public class HomekitCameraOperatingModeService extends AbstractHomekitService {
    // ========== Log Message Prefixes ==========
    private static final String LOG_PREFIX = "Homekit CameraOperatingModeService: ";
    private static final String LOG_INIT = LOG_PREFIX + "Init - ";
    private static final String LOG_STATE = LOG_PREFIX + "State - ";
    private static final String LOG_TRACE = LOG_PREFIX + "Trace - ";

    private static final Logger logger = LoggerFactory.getLogger(HomekitCameraOperatingModeService.class);

    /**
     * Creates a new Camera Operating Mode service.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @since 1.0
     */
    public HomekitCameraOperatingModeService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory) {
        super(accessory, eventManager, characteristicFactory);
        withName("Camera Operating Mode").withExtensible(true).withPrimary(false).withHidden(false);
        logger.debug("{}Created CameraOperatingModeService for accessory {}", LOG_INIT, accessory.getLabel());
    }

    /**
     * Creates a new Camera Operating Mode service from a JSON configuration.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @param value JSON value containing service configuration
     * @since 1.0
     */
    public HomekitCameraOperatingModeService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory, JsonValue value) {
        super(accessory, eventManager, characteristicFactory, value);
        logger.debug("{}Created CameraOperatingModeService from JSON for accessory {}", LOG_INIT, accessory.getLabel());
    }

    /**
     * Adds the required characteristics for this service.
     * <p>
     * Required characteristics:
     * <ul>
     * <li>CameraOperatingModeIndicator - Current operating mode status</li>
     * </ul>
     * </p>
     * 
     * @since 1.0
     */
    @Override
    public void addCharacteristics() throws HomekitServiceException {
        logger.debug("{}Adding required characteristics to CameraOperatingModeService for accessory {}", LOG_TRACE,
                getAccessory().getLabel());

        // Required characteristics
        addCharacteristic(new HomekitCameraOperatingModeIndicatorCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        logger.debug("{}Added CameraOperatingModeIndicatorCharacteristic to CameraOperatingModeService", LOG_STATE);
    }
}
