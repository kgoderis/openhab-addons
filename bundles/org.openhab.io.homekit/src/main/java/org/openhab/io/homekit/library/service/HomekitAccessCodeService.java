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
import org.openhab.io.homekit.library.characteristic.HomekitAccessCodeControlPointCharacteristic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HomeKit Access Code Service.
 * 
 * <p>
 * This service provides control over access codes in HomeKit, including:
 * <ul>
 * <li>Access code management and control</li>
 * <li>Secure code storage and retrieval</li>
 * <li>Access code state monitoring</li>
 * </ul>
 * </p>
 *
 * <p>
 * The service is used to:
 * <ul>
 * <li>Manage access codes for secure entry systems</li>
 * <li>Control access code operations</li>
 * <li>Monitor access code states</li>
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
@HomekitServiceType(type = "00000260-0000-1000-8000-0026BB765291", name = "AccessCode", tag = "accessCode")
@NonNullByDefault
public class HomekitAccessCodeService extends AbstractHomekitService {
    // ========== Log Message Prefixes ==========
    private static final String LOG_PREFIX = "Homekit AccessCodeService: ";
    private static final String LOG_INIT = LOG_PREFIX + "Init - ";
    private static final String LOG_CONFIG = LOG_PREFIX + "Config - ";

    private final Logger logger = LoggerFactory.getLogger(HomekitAccessCodeService.class);

    /**
     * Creates a new Access Code service.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @since 1.0
     */
    public HomekitAccessCodeService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory) {
        super(accessory, eventManager, characteristicFactory);
        withName("Access Code").withExtensible(true).withPrimary(false).withHidden(false);
        logger.debug("{}Created new Access Code service for accessory {}", LOG_INIT, accessory.getLabel());
    }

    /**
     * Creates a new Access Code service from a JSON configuration.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @param value JSON value containing service configuration
     * @since 1.0
     */
    public HomekitAccessCodeService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory, JsonValue value) {
        super(accessory, eventManager, characteristicFactory, value);
        logger.debug("{}Created new Access Code service from JSON configuration for accessory {}", LOG_INIT,
                accessory.getLabel());
    }

    /**
     * Adds the required characteristics for this service.
     * 
     * <p>
     * Required characteristics:
     * <ul>
     * <li>AccessCodeControlPoint (UUID: 00000261-0000-1000-8000-0026BB765291)</li>
     * </ul>
     * </p>
     *
     * <p>
     * The AccessCodeControlPoint characteristic provides:
     * <ul>
     * <li>Access code management operations</li>
     * <li>Code state control</li>
     * <li>Secure code handling</li>
     * </ul>
     * </p>
     *
     * @since 1.0
     */
    @Override
    public void addCharacteristics() throws HomekitServiceException {
        // Required characteristics
        addCharacteristic(new HomekitAccessCodeControlPointCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        logger.debug("{}Added required characteristics for Access Code service", LOG_CONFIG);
    }
}
