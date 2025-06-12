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
import org.openhab.io.homekit.library.characteristic.HomekitRecordingAudioActiveCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitSelectedCameraRecordingConfigurationCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitSupportedAudioRecordingConfigurationCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitSupportedCameraRecordingConfigurationCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitSupportedVideoRecordingConfigurationCharacteristic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HomeKit Camera Recording Management Service.
 * <p>
 * This service provides comprehensive control over camera recording settings and configurations in HomeKit accessories.
 * It manages recording parameters, audio settings, and configuration options for IP cameras and other video recording
 * devices.
 * </p>
 *
 * <ul>
 * <li>Recording configuration management</li>
 * <li>Video and audio recording parameter control</li>
 * <li>Recording state and status monitoring</li>
 * <li>Audio recording control</li>
 * </ul>
 *
 * <p>
 * Required characteristics:
 * <ul>
 * <li>Active - Service activation state</li>
 * <li>SelectedCameraRecordingConfiguration - Current recording settings</li>
 * <li>SupportedAudioRecordingConfiguration - Available audio recording options</li>
 * <li>SupportedCameraRecordingConfiguration - Available camera recording options</li>
 * <li>SupportedVideoRecordingConfiguration - Available video recording options</li>
 * </ul>
 * </p>
 *
 * <p>
 * Optional characteristics:
 * <ul>
 * <li>RecordingAudioActive - Audio recording state</li>
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
@HomekitServiceType(type = "00000204-0000-1000-8000-0026BB765291", name = "CameraRecordingManagement", tag = "cameraRecordingManagement")
@NonNullByDefault
public class HomekitCameraRecordingManagementService extends AbstractHomekitService {
    // ========== Log Message Prefixes ==========
    private static final String LOG_PREFIX = "Homekit CameraRecordingManagementService: ";
    private static final String LOG_INIT = LOG_PREFIX + "Init - ";
    private static final String LOG_STATE = LOG_PREFIX + "State - ";
    private static final String LOG_TRACE = LOG_PREFIX + "Trace - ";

    private final Logger logger = LoggerFactory.getLogger(HomekitCameraRecordingManagementService.class);

    /**
     * Creates a new Camera Recording Management service.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @since 1.0
     */
    public HomekitCameraRecordingManagementService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory) {
        super(accessory, eventManager, characteristicFactory);
        withName("Camera Recording Management").withExtensible(true).withPrimary(false).withHidden(false);
        logger.debug("{}Created CameraRecordingManagementService for accessory {}", LOG_INIT, accessory.getLabel());
    }

    /**
     * Creates a new Camera Recording Management service from a JSON configuration.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @param value JSON value containing service configuration
     * @since 1.0
     */
    public HomekitCameraRecordingManagementService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory, JsonValue value) {
        super(accessory, eventManager, characteristicFactory, value);
        logger.debug("{}Created CameraRecordingManagementService from JSON for accessory {}", LOG_INIT,
                accessory.getLabel());
    }

    /**
     * Adds the required and optional characteristics for this service.
     * <p>
     * Required characteristics:
     * <ul>
     * <li>Active - Service activation state</li>
     * <li>SelectedCameraRecordingConfiguration - Current recording settings</li>
     * <li>SupportedAudioRecordingConfiguration - Available audio recording options</li>
     * <li>SupportedCameraRecordingConfiguration - Available camera recording options</li>
     * <li>SupportedVideoRecordingConfiguration - Available video recording options</li>
     * </ul>
     * </p>
     * <p>
     * Optional characteristics:
     * <ul>
     * <li>RecordingAudioActive - Audio recording state</li>
     * </ul>
     * </p>
     * 
     * @since 1.0
     */
    @Override
    public void addCharacteristics() throws HomekitServiceException {
        logger.trace("{}Adding required characteristics to CameraRecordingManagementService for accessory {}",
                LOG_TRACE, getAccessory().getLabel());

        // Required characteristics
        addCharacteristic(
                new HomekitActiveCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(true));
        logger.debug("{}Added ActiveCharacteristic to CameraRecordingManagementService", LOG_STATE);

        addCharacteristic(new HomekitSelectedCameraRecordingConfigurationCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        logger.debug("{}Added SelectedCameraRecordingConfigurationCharacteristic to CameraRecordingManagementService",
                LOG_STATE);

        addCharacteristic(new HomekitSupportedAudioRecordingConfigurationCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        logger.debug("{}Added SupportedAudioRecordingConfigurationCharacteristic to CameraRecordingManagementService",
                LOG_STATE);

        addCharacteristic(new HomekitSupportedCameraRecordingConfigurationCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        logger.debug("{}Added SupportedCameraRecordingConfigurationCharacteristic to CameraRecordingManagementService",
                LOG_STATE);

        addCharacteristic(new HomekitSupportedVideoRecordingConfigurationCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        logger.debug("{}Added SupportedVideoRecordingConfigurationCharacteristic to CameraRecordingManagementService",
                LOG_STATE);

        // Optional characteristics
        addCharacteristic(new HomekitRecordingAudioActiveCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(false));
        logger.debug("{}Added RecordingAudioActiveCharacteristic to CameraRecordingManagementService", LOG_STATE);
    }
}
