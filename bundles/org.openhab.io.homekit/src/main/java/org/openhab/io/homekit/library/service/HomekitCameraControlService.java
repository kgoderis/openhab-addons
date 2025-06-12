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
import org.openhab.io.homekit.library.characteristic.HomekitSelectedRTPStreamConfigurationCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitSetupEndpointsCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitStreamingStatusCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitSupportedAudioStreamConfigurationCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitSupportedRTPConfigurationCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitSupportedVideoStreamConfigurationCharacteristic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HomeKit Camera Control Service.
 * <p>
 * This service provides comprehensive control over camera RTP (Real-time Transport Protocol) stream settings
 * and configurations in HomeKit accessories. It manages video and audio streaming parameters, endpoints,
 * and streaming status for IP cameras and other video-enabled devices.
 * </p>
 *
 * <ul>
 * <li>RTP stream configuration management</li>
 * <li>Video and audio stream parameter control</li>
 * <li>Streaming endpoint setup and management</li>
 * <li>Streaming status monitoring</li>
 * </ul>
 *
 * <p>
 * Required characteristics:
 * <ul>
 * <li>SelectedRTPStreamConfiguration - Current RTP stream settings</li>
 * <li>SetupEndpoints - Stream endpoint configuration</li>
 * <li>StreamingStatus - Current streaming state</li>
 * <li>SupportedAudioStreamConfiguration - Available audio stream options</li>
 * <li>SupportedRTPConfiguration - Available RTP protocol options</li>
 * <li>SupportedVideoStreamConfiguration - Available video stream options</li>
 * </ul>
 * </p>
 *
 * <p>
 * Optional characteristics:
 * <ul>
 * <li>Active - Service activation state</li>
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
@HomekitServiceType(type = "00000110-0000-1000-8000-0026BB765291", name = "Camera RTP Stream Management", tag = "cameraRTPStreamManagement")
@NonNullByDefault
public class HomekitCameraControlService extends AbstractHomekitService {
    // ========== Log Message Prefixes ==========
    private static final String LOG_PREFIX = "Homekit CameraControlService: ";
    private static final String LOG_INIT = LOG_PREFIX + "Init - ";
    private static final String LOG_STATE = LOG_PREFIX + "State - ";
    private static final String LOG_TRACE = LOG_PREFIX + "Trace - ";

    private final Logger logger = LoggerFactory.getLogger(HomekitCameraControlService.class);

    /**
     * Creates a new Camera Control service.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @since 1.0
     */
    public HomekitCameraControlService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory) {
        super(accessory, eventManager, characteristicFactory);
        withName("Camera RTP Stream Management").withExtensible(false).withPrimary(false).withHidden(false);
        logger.debug("{}Created CameraControlService for accessory {}", LOG_INIT, accessory.getLabel());
    }

    /**
     * Creates a new Camera Control service from a JSON configuration.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @param value JSON value containing service configuration
     * @since 1.0
     */
    public HomekitCameraControlService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory, JsonValue value) {
        super(accessory, eventManager, characteristicFactory, value);
        logger.debug("{}Created CameraControlService from JSON for accessory {}", LOG_INIT, accessory.getLabel());
    }

    /**
     * Adds the required and optional characteristics for this service.
     * <p>
     * Required characteristics:
     * <ul>
     * <li>SelectedRTPStreamConfiguration - Current RTP stream settings</li>
     * <li>SetupEndpoints - Stream endpoint configuration</li>
     * <li>StreamingStatus - Current streaming state</li>
     * <li>SupportedAudioStreamConfiguration - Available audio stream options</li>
     * <li>SupportedRTPConfiguration - Available RTP protocol options</li>
     * <li>SupportedVideoStreamConfiguration - Available video stream options</li>
     * </ul>
     * </p>
     * <p>
     * Optional characteristics:
     * <ul>
     * <li>Active - Service activation state</li>
     * </ul>
     * </p>
     * 
     * @since 1.0
     */
    @Override
    public void addCharacteristics() throws HomekitServiceException {
        logger.trace("{}Adding required characteristics to CameraControlService for accessory {}", LOG_TRACE,
                getAccessory().getLabel());

        // Required characteristics
        addCharacteristic(new HomekitSelectedRTPStreamConfigurationCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        logger.debug("{}Added SelectedRTPStreamConfigurationCharacteristic to CameraControlService", LOG_STATE);

        addCharacteristic(
                new HomekitSetupEndpointsCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(true));
        logger.debug("{}Added SetupEndpointsCharacteristic to CameraControlService", LOG_STATE);

        addCharacteristic(new HomekitStreamingStatusCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        logger.debug("{}Added StreamingStatusCharacteristic to CameraControlService", LOG_STATE);

        addCharacteristic(new HomekitSupportedAudioStreamConfigurationCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        logger.debug("{}Added SupportedAudioStreamConfigurationCharacteristic to CameraControlService", LOG_STATE);

        addCharacteristic(new HomekitSupportedRTPConfigurationCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        logger.debug("{}Added SupportedRTPConfigurationCharacteristic to CameraControlService", LOG_STATE);

        addCharacteristic(new HomekitSupportedVideoStreamConfigurationCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        logger.debug("{}Added SupportedVideoStreamConfigurationCharacteristic to CameraControlService", LOG_STATE);

        // Optional characteristics
        addCharacteristic(
                new HomekitActiveCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(false));
        logger.debug("{}Added ActiveCharacteristic to CameraControlService", LOG_STATE);
    }

    /**
     * Indicates that this service is not extensible.
     * Camera control service has a fixed set of characteristics.
     *
     * @return false, as this service is not extensible
     * @since 1.0
     */
    @Override
    public boolean isExtensible() {
        return false;
    }
}
