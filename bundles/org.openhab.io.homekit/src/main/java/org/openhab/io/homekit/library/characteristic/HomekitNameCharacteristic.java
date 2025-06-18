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

package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitReadOnlyStringCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HomeKit Name Characteristic.
 * 
 * <p>
 * This characteristic represents the display name of a device.
 * It is a read-only string value that provides a user-friendly name for the accessory.
 * </p>
 *
 * <p>
 * The name is used to:
 * <ul>
 * <li>Provide a human-readable identifier for the device</li>
 * <li>Enable easy device identification in HomeKit apps</li>
 * <li>Support device organization and management</li>
 * <li>Facilitate user interaction and control</li>
 * </ul>
 * </p>
 *
 * @author Karel Goderis - Initial contribution
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "00000023-0000-1000-8000-0026BB765291", name = "Name", tag = "name", acceptedItemTypes = {
        "String", "Text" })
@NonNullByDefault
public class HomekitNameCharacteristic extends HomekitReadOnlyStringCharacteristic {
    // ========== Log Message Prefixes ==========
    private static final String LOG_PREFIX = "Homekit NameCharacteristic: ";
    private static final String LOG_INIT = LOG_PREFIX + "Init - ";

    private static final Logger logger = LoggerFactory.getLogger(HomekitNameCharacteristic.class);

    /**
     * Creates a new Name characteristic.
     * 
     * <p>
     * This characteristic is read-only and provides the display name of the accessory.
     * </p>
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param instanceId The instance ID for this characteristic
     */
    public HomekitNameCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(false).withEvents(false)
                .withDescription("Name");
        logger.debug("{}Created new Name characteristic with instance ID {}", LOG_INIT, instanceId);
    }

    /**
     * Creates a new Name characteristic from a JSON value.
     * 
     * <p>
     * This constructor is used when restoring a characteristic from persistent storage.
     * </p>
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param value The JSON value to initialize the characteristic with
     */
    public HomekitNameCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
        logger.debug("{}Restored Name characteristic from JSON", LOG_INIT);
    }
}
