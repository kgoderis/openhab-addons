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

package org.openhab.io.homekit.core.accessory;

import javax.json.JsonValue;

import org.openhab.io.homekit.api.accessory.HomekitAccessoryType;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.factory.HomekitServiceFactory;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A flexible implementation of a HomeKit accessory that can be customized for various use cases.
 * This class provides a foundation for creating custom HomeKit accessories with configurable
 * services, characteristics, and behavior.
 *
 * <p>
 * The generic accessory serves as a versatile base for implementing custom HomeKit devices,
 * offering:
 * <ul>
 * <li>Configurable accessory information (name, manufacturer, model, etc.)</li>
 * <li>Dynamic service management and characteristic configuration</li>
 * <li>Event handling and state synchronization</li>
 * <li>JSON serialization for persistence and configuration</li>
 * </ul>
 * </p>
 *
 * <p>
 * Key features:
 * <ul>
 * <li>Customizable accessory metadata and identification</li>
 * <li>Dynamic service and characteristic management</li>
 * <li>Event propagation and state updates</li>
 * <li>Extensible service configuration</li>
 * <li>Persistence through JSON serialization</li>
 * </ul>
 * </p>
 *
 * <p>
 * The class integrates with several key components:
 * <ul>
 * <li>{@link AbstractHomekitAccessory} - Base accessory functionality</li>
 * <li>{@link HomekitService} - Service management</li>
 * <li>{@link HomekitAccessoryType} - Accessory type definition</li>
 * <li>{@link HomekitAccessoryUID} - Unique identification</li>
 * <li>{@link HomekitAccessoryServer} - Server integration</li>
 * </ul>
 * </p>
 *
 * <p>
 * The generic accessory supports dynamic configuration through JSON, allowing for:
 * <ul>
 * <li>Service addition and removal</li>
 * <li>Characteristic configuration</li>
 * <li>Accessory metadata updates</li>
 * <li>State persistence</li>
 * </ul>
 * </p>
 *
 * @author Karel Goderis - Initial contribution
 * @version 1.0
 * @since 1.0
     */
@HomekitAccessoryType(name = "Generic Accessory", type = "1110001-0000-1000-8000-0026BB765291", tag = "generic")
public class HomekitGenericAccessory extends AbstractHomekitAccessory {
    // ========== Log Message Prefixes ==========
    private static final String LOG_PREFIX = "Homekit GenericAccessory: ";
    private static final String LOG_INIT = LOG_PREFIX + "Init - ";

    private final Logger logger = LoggerFactory.getLogger(HomekitGenericAccessory.class);

    /**
     * Creates a new generic HomeKit accessory with the specified factories and event manager.
     * This constructor initializes a new accessory with default settings.
     *
     * @param eventManager The event manager for handling HomeKit events
     * @param serviceFactory The factory for creating HomeKit services
     * @param characteristicFactory The factory for creating HomeKit characteristics
     * @see HomekitEventManager
     * @see HomekitServiceFactory
     * @see HomekitCharacteristicFactory
     */
    public HomekitGenericAccessory(HomekitEventManager eventManager, HomekitServiceFactory serviceFactory,
            HomekitCharacteristicFactory characteristicFactory) {
        super(eventManager, serviceFactory, characteristicFactory);
        logger.debug("{}Created new generic accessory", LOG_INIT);
    }

    /**
     * Creates a new generic HomeKit accessory from a JSON value.
     * This constructor is used when restoring an accessory from persistent storage.
     *
     * @param eventManager The event manager for handling HomeKit events
     * @param serviceFactory The factory for creating HomeKit services
     * @param characteristicFactory The factory for creating HomeKit characteristics
     * @param value The JSON value containing the accessory configuration
     * @see HomekitEventManager
     * @see HomekitServiceFactory
     * @see HomekitCharacteristicFactory
     * @see JsonValue
     */
    public HomekitGenericAccessory(HomekitEventManager eventManager, HomekitServiceFactory serviceFactory,
            HomekitCharacteristicFactory characteristicFactory, JsonValue value) {
        super(eventManager, serviceFactory, characteristicFactory, value);
        logger.debug("{}Restored generic accessory from JSON", LOG_INIT);
    }
}
