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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.service.HomekitServiceType;
import org.openhab.io.homekit.core.service.AbstractHomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.exception.HomekitServiceException;
import org.openhab.io.homekit.library.characteristic.HomekitActiveCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitIdentifierCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitNameCharacteristic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HomeKit Assistant Service.
 * <p>
 * This service represents the Assistant functionality in HomeKit, providing voice assistant capabilities
 * and integration with Siri and other voice assistants. It manages the state and configuration of
 * voice assistant features on HomeKit accessories.
 * </p>
 *
 * <ul>
 * <li>Voice assistant activation control</li>
 * <li>Assistant identification and naming</li>
 * <li>Integration with HomeKit event system</li>
 * </ul>
 *
 * <p>
 * Required characteristics:
 * <ul>
 * <li>Active - Controls whether the assistant is active</li>
 * <li>Identifier - Unique identifier for the assistant</li>
 * <li>Name - Display name for the assistant</li>
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
@HomekitServiceType(type = "0000026A-0000-1000-8000-0026BB765291", name = "Assistant", tag = "Assistant")
@NonNullByDefault
public class HomekitAssistantService extends AbstractHomekitService {
    // ========== Log Message Prefixes ==========
    private static final String LOG_PREFIX = "Homekit AssistantService: ";
    private static final String LOG_INIT = LOG_PREFIX + "Init - ";
    private static final String LOG_STATE = LOG_PREFIX + "State - ";
    private static final String LOG_TRACE = LOG_PREFIX + "Trace - ";

    private static final Logger logger = LoggerFactory.getLogger(HomekitAssistantService.class);

    /**
     * Creates a new Assistant service.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @since 1.0
     */
    public HomekitAssistantService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory) {
        super(accessory, eventManager, characteristicFactory);
        withName("Assistant").withExtensible(true).withPrimary(false).withHidden(false);
        logger.debug("{}Created AssistantService for accessory {}", LOG_INIT, accessory.getLabel());
    }

    /**
     * Adds the required characteristics for this service.
     * <p>
     * Required characteristics:
     * <ul>
     * <li>Active - Controls whether the assistant is active</li>
     * <li>Identifier - Unique identifier for the assistant</li>
     * <li>Name - Display name for the assistant</li>
     * </ul>
     * </p>
     * 
     * @since 1.0
     */
    @Override
    public void addCharacteristics() throws HomekitServiceException {
        logger.debug("{}Adding required characteristics to AssistantService for accessory {}", LOG_TRACE,
                getAccessory().getLabel());

        // Required characteristics
        addCharacteristic(
                new HomekitActiveCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(true));
        logger.debug("{}Added ActiveCharacteristic to AssistantService", LOG_STATE);

        addCharacteristic(
                new HomekitIdentifierCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(true));
        logger.debug("{}Added IdentifierCharacteristic to AssistantService", LOG_STATE);

        addCharacteristic(new HomekitNameCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                .withMandatory(true));
        logger.debug("{}Added NameCharacteristic to AssistantService", LOG_STATE);
    }
}
