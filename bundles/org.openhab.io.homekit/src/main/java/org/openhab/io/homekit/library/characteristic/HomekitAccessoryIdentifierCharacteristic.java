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
import org.openhab.io.homekit.core.characteristic.HomekitStringCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HomeKit Accessory Identifier Characteristic.
 * 
 * <p>
 * This characteristic represents the unique identifier of an accessory.
 * It is a read-only string value that uniquely identifies the accessory in the Home app.
 * </p>
 *
 * <p>
 * The identifier is used to:
 * <ul>
 * <li>Uniquely identify the accessory in the HomeKit ecosystem</li>
 * <li>Maintain consistent identification across reboots and reconfigurations</li>
 * <li>Enable proper pairing and communication with HomeKit clients</li>
 * </ul>
 * </p>
 *
 * @author Karel Goderis - Initial contribution
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "00000057-0000-1000-8000-0026BB765291", name = "Accessory Identifier", tag = "accessoryIdentifier", acceptedItemTypes = {
        "String", "Text" })
@NonNullByDefault
public class HomekitAccessoryIdentifierCharacteristic extends HomekitStringCharacteristic {
    // ========== Log Message Prefixes ==========
    private static final String LOG_PREFIX = "Homekit AccessoryIdentifierCharacteristic: ";
    private static final String LOG_INIT = LOG_PREFIX + "Init - ";

    private static final Logger logger = LoggerFactory.getLogger(HomekitAccessoryIdentifierCharacteristic.class);

    /**
     * Creates a new Accessory Identifier characteristic.
     * 
     * <p>
     * This characteristic is read-only and provides a unique identifier for the accessory.
     * </p>
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param instanceId The instance ID for this characteristic
     */
    public HomekitAccessoryIdentifierCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(false).withEvents(false)
                .withDescription("Accessory Identifier");
        logger.debug("{}Created new Accessory Identifier characteristic with instance ID {}", LOG_INIT, instanceId);
    }

    /**
     * Creates a new Accessory Identifier characteristic from a JSON value.
     * 
     * <p>
     * This constructor is used when restoring a characteristic from persistent storage.
     * </p>
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param value The JSON value to initialize the characteristic with
     */
    public HomekitAccessoryIdentifierCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
        logger.debug("{}Restored Accessory Identifier characteristic from JSON", LOG_INIT);
    }
}
