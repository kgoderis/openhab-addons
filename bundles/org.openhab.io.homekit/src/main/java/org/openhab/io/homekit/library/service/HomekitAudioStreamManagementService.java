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
import org.openhab.io.homekit.library.characteristic.HomekitSelectedAudioStreamConfigurationCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitSupportedAudioStreamConfigurationCharacteristic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HomeKit Audio Stream Management Service.
 * <p>
 * This service provides control over audio stream configurations and settings in HomeKit accessories.
 * It manages audio stream parameters, quality settings, and configuration options for audio-enabled devices.
 * </p>
 *
 * <ul>
 * <li>Audio stream configuration management</li>
 * <li>Stream quality and parameter control</li>
 * <li>Support for multiple audio configurations</li>
 * </ul>
 *
 * <p>
 * Required characteristics:
 * <ul>
 * <li>SelectedAudioStreamConfiguration - Current audio stream settings</li>
 * <li>SupportedAudioStreamConfiguration - Available audio stream options</li>
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
@HomekitServiceType(type = "00000127-0000-1000-8000-0026BB765291", name = "Audio Stream Management", tag = "audioStreamManagement")
@NonNullByDefault
public class HomekitAudioStreamManagementService extends AbstractHomekitService {
    // ========== Log Message Prefixes ==========
    private static final String LOG_PREFIX = "Homekit AudioStreamManagementService: ";
    private static final String LOG_INIT = LOG_PREFIX + "Init - ";
    private static final String LOG_STATE = LOG_PREFIX + "State - ";
    private static final String LOG_TRACE = LOG_PREFIX + "Trace - ";
    private static final String LOG_CONFIG = LOG_PREFIX + "Config - ";

    private static final Logger logger = LoggerFactory.getLogger(HomekitAudioStreamManagementService.class);

    /**
     * Creates a new Audio Stream Management service.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @since 1.0
     */
    public HomekitAudioStreamManagementService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory) {
        super(accessory, eventManager, characteristicFactory);
        withName("Audio Stream Management").withPrimary(false).withHidden(false).withExtensible(false);
        logger.debug("{}Created AudioStreamManagementService for accessory {}", LOG_INIT, accessory.getLabel());
    }

    /**
     * Creates a new Audio Stream Management service from a JSON configuration.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @param value JSON value containing service configuration
     * @since 1.0
     */
    public HomekitAudioStreamManagementService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory, JsonValue value) {
        super(accessory, eventManager, characteristicFactory, value);
        logger.debug("{}Created AudioStreamManagementService from JSON for accessory {}", LOG_INIT,
                accessory.getLabel());
    }

    /**
     * Adds the required characteristics for this service.
     * <p>
     * Required characteristics:
     * <ul>
     * <li>SelectedAudioStreamConfiguration - Current audio stream settings</li>
     * <li>SupportedAudioStreamConfiguration - Available audio stream options</li>
     * </ul>
     * </p>
     * 
     * @since 1.0
     */
    @Override
    public void addCharacteristics() throws HomekitServiceException {
        logger.debug("{}Adding required characteristics to AudioStreamManagementService for accessory {}", LOG_TRACE,
                getAccessory().getLabel());

        addCharacteristic(new HomekitSelectedAudioStreamConfigurationCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        logger.debug("{}Added SelectedAudioStreamConfigurationCharacteristic to AudioStreamManagementService",
                LOG_STATE);

        addCharacteristic(new HomekitSupportedAudioStreamConfigurationCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        logger.debug("{}Added SupportedAudioStreamConfigurationCharacteristic to AudioStreamManagementService",
                LOG_STATE);

        logger.debug("{}Added characteristics for Audio Stream Management service", LOG_CONFIG);
    }
}
