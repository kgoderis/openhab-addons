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
import org.openhab.io.homekit.library.characteristic.HomekitAssetUpdateReadinessCharacteristic;

/**
 * HomeKit Asset Update Service.
 * <p>
 * This service provides functionality for updating assets in HomeKit, such as firmware or configuration updates.
 * It exposes characteristics that allow clients to check readiness and status of asset updates.
 * </p>
 *
 * <ul>
 * <li>Asset update readiness monitoring</li>
 * <li>Integration with HomeKit event system</li>
 * <li>Extensible for future asset update features</li>
 * </ul>
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
@HomekitServiceType(type = "00000267-0000-1000-8000-0026BB765291", name = "AssetUpdate", tag = "assetUpdate")
@NonNullByDefault
public class HomekitAssetUpdateService extends AbstractHomekitService {
    // ========== Log Message Prefixes ==========
    private static final String LOG_PREFIX = "Homekit AssetUpdateService: ";
    private static final String LOG_INIT = LOG_PREFIX + "Init - ";
    private static final String LOG_STATE = LOG_PREFIX + "State - ";
    private static final String LOG_TRACE = LOG_PREFIX + "Trace - ";

    private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(HomekitAssetUpdateService.class);

    /**
     * Creates a new Asset Update service.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @since 1.0
     */
    public HomekitAssetUpdateService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory) {
        super(accessory, eventManager, characteristicFactory);
        withName("Asset Update").withExtensible(true).withPrimary(false).withHidden(false);
        logger.debug("{}Created AssetUpdateService for accessory {}", LOG_INIT, accessory.getLabel());
    }

    /**
     * Creates a new Asset Update service from a JSON configuration.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @param value JSON value containing service configuration
     * @since 1.0
     */
    public HomekitAssetUpdateService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory, JsonValue value) {
        super(accessory, eventManager, characteristicFactory, value);
        logger.debug("{}Created AssetUpdateService from JSON for accessory {}", LOG_INIT, accessory.getLabel());
    }

    /**
     * Adds the required characteristics for this service.
     * <p>
     * Required characteristics:
     * <ul>
     * <li>AssetUpdateReadiness</li>
     * </ul>
     * </p>
     * 
     * @since 1.0
     */
    @Override
    public void addCharacteristics() throws HomekitServiceException {
        logger.trace("{}Adding required characteristics to AssetUpdateService for accessory {}", LOG_TRACE,
                getAccessory().getLabel());
        // Required characteristics
        addCharacteristic(new HomekitAssetUpdateReadinessCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        logger.debug("{}Added AssetUpdateReadinessCharacteristic to AssetUpdateService", LOG_STATE);
    }
}
