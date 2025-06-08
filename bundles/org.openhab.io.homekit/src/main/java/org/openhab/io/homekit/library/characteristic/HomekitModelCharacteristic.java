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
 * HomeKit Model Characteristic.
 * 
 * <p>
 * This characteristic represents the model name of the accessory.
 * It is a read-only string value that identifies the specific model of the device.
 * </p>
 *
 * <p>
 * The model name is used to:
 * <ul>
 * <li>Identify the specific model of the accessory</li>
 * <li>Help users identify and organize their accessories</li>
 * <li>Provide context for support and troubleshooting</li>
 * <li>Enable proper device-specific functionality</li>
 * </ul>
 * </p>
 *
 * @author Karel Goderis - Initial contribution
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "00000021-0000-1000-8000-0026BB765291", name = "Model", tag = "model", acceptedItemTypes = {
        "String", "Text" })
@NonNullByDefault
public class HomekitModelCharacteristic extends HomekitStringCharacteristic {
    // ========== Log Message Prefixes ==========
    private static final String LOG_PREFIX = "Homekit ModelCharacteristic: ";
    private static final String LOG_INIT = LOG_PREFIX + "Init - ";

    private final Logger logger = LoggerFactory.getLogger(HomekitModelCharacteristic.class);

    /**
     * Creates a new Model characteristic.
     * 
     * <p>
     * This characteristic is read-only and provides the model name of the accessory.
     * </p>
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param instanceId The instance ID for this characteristic
     */
    public HomekitModelCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(false)
                .withDescription("Model");
        logger.debug("{}Created new Model characteristic with instance ID {}", LOG_INIT, instanceId);
    }

    /**
     * Creates a new Model characteristic from a JSON value.
     * 
     * <p>
     * This constructor is used when restoring a characteristic from persistent storage.
     * </p>
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param value The JSON value to initialize the characteristic with
     */
    public HomekitModelCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
        logger.debug("{}Restored Model characteristic from JSON", LOG_INIT);
    }
}
