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
import org.openhab.io.homekit.library.characteristic.HomekitMetricsBufferFullStateCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitSupportedMetricsCharacteristic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HomeKit Accessory Metrics Service.
 * 
 * <p>
 * This service provides metrics and diagnostic information about the accessory, including:
 * <ul>
 * <li>Performance metrics collection</li>
 * <li>Diagnostic data monitoring</li>
 * <li>Buffer state management</li>
 * </ul>
 * </p>
 *
 * <p>
 * The service is used to:
 * <ul>
 * <li>Track accessory performance metrics</li>
 * <li>Monitor diagnostic information</li>
 * <li>Manage metrics buffer states</li>
 * <li>Report supported metrics capabilities</li>
 * </ul>
 * </p>
 *
 * <p>
 * For more information, see the
 * <a href="https://developer.apple.com/documentation/HomeKit">HomeKit Accessory Protocol Specification</a>.
 * </p>
 *
 * @author Karel Goderis - Initial contribution
 * @version 1.0
 * @since 1.0
 */
@HomekitServiceType(type = "00000270-0000-1000-8000-0026BB765291", name = "AccessoryMetrics", tag = "accessoryMetrics")
@NonNullByDefault
public class HomekitAccessoryMetricsService extends AbstractHomekitService {
    // ========== Log Message Prefixes ==========
    private static final String LOG_PREFIX = "Homekit AccessoryMetricsService: ";
    private static final String LOG_INIT = LOG_PREFIX + "Init - ";
    private static final String LOG_CONFIG = LOG_PREFIX + "Config - ";

    private final Logger logger = LoggerFactory.getLogger(HomekitAccessoryMetricsService.class);

    /**
     * Creates a new Accessory Metrics service.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @since 1.0
     */
    public HomekitAccessoryMetricsService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory) {
        super(accessory, eventManager, characteristicFactory);
        withName("Accessory Metrics").withExtensible(true).withPrimary(false).withHidden(false);
        logger.debug("{}Created new Accessory Metrics service for accessory {}", LOG_INIT, accessory.getLabel());
    }

    /**
     * Creates a new Accessory Metrics service from a JSON configuration.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @param value JSON value containing service configuration
     * @since 1.0
     */
    public HomekitAccessoryMetricsService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory, JsonValue value) {
        super(accessory, eventManager, characteristicFactory, value);
        logger.debug("{}Created new Accessory Metrics service from JSON configuration for accessory {}", LOG_INIT,
                accessory.getLabel());
    }

    /**
     * Adds the required characteristics for this service.
     * 
     * <p>
     * Required characteristics:
     * <ul>
     * <li>Active (UUID: 000000B0-0000-1000-8000-0026BB765291)</li>
     * <li>MetricsBufferFullState (UUID: 00000271-0000-1000-8000-0026BB765291)</li>
     * <li>SupportedMetrics (UUID: 00000272-0000-1000-8000-0026BB765291)</li>
     * </ul>
     * </p>
     *
     * <p>
     * These characteristics provide:
     * <ul>
     * <li>Service activation state</li>
     * <li>Metrics buffer capacity status</li>
     * <li>Supported metrics capabilities</li>
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
        addCharacteristic(new HomekitMetricsBufferFullStateCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        addCharacteristic(new HomekitSupportedMetricsCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        logger.debug("{}Added required characteristics for Accessory Metrics service", LOG_CONFIG);
    }
}
