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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.accessory.HomekitAccessoryType;
import org.openhab.io.homekit.api.factory.HomekitAccessoryFactory;
import org.openhab.io.homekit.api.service.HomekitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a HomeKit accessory that has been persisted to storage.
 * This class serves as a data transfer object (DTO) for storing and retrieving
 * HomeKit accessory configurations from persistent storage.
 *
 * <p>
 * The persisted accessory maintains:
 * <ul>
 * <li>The accessory type for proper restoration</li>
 * <li>A JSON representation of the accessory's state</li>
 * <li>Configuration data for service and characteristic setup</li>
 * <li>Metadata for accessory identification</li>
 * </ul>
 * </p>
 *
 * <p>
 * Key features:
 * <ul>
 * <li>Type-safe accessory restoration</li>
 * <li>Complete state preservation</li>
 * <li>JSON-based serialization</li>
 * <li>Configuration persistence</li>
 * </ul>
 * </p>
 *
 * <p>
 * The class integrates with several key components:
 * <ul>
 * <li>{@link HomekitAccessory} - Base accessory interface</li>
 * <li>{@link HomekitAccessoryFactory} - Accessory creation</li>
 * <li>{@link HomekitService} - Service management</li>
 * <li>{@link JsonObject} - State serialization</li>
 * </ul>
 * </p>
 *
 * <p>
 * The persisted accessory is used by the
 * {@link HomekitPersistedAccessoryProvider}
 * to maintain accessory configurations across system restarts and to support
 * dynamic accessory management.
 * </p>
 *
 * @author Karel Goderis - Initial contribution
 * @version 1.0
 * @since 1.0
 */
@NonNullByDefault
public class HomekitPersistedAccessory {
    // ========== Log Message Prefixes ==========
    private static final String LOG_PREFIX = "Homekit PersistedAccessory: ";
    private static final String LOG_INIT = LOG_PREFIX + "Init - ";
    private static final String LOG_STATE = LOG_PREFIX + "State - ";

    private final Logger logger = LoggerFactory.getLogger(HomekitPersistedAccessory.class);

    private String json;
    private String accessoryType;

    /**
     * Creates a new empty persisted accessory.
     * This constructor initializes all fields with empty values.
     */
    public HomekitPersistedAccessory() {
        json = "";
        accessoryType = "";
        logger.debug("{}Created new empty persisted accessory", LOG_INIT);
    }

    /**
     * Creates a new persisted accessory with the specified type and JSON data.
     * This constructor initializes the persisted accessory with the information
     * needed to restore it later.
     *
     * @param accessoryType The type of the accessory
     * @param json The JSON representation of the accessory
     * @throws IllegalArgumentException if either parameter is null
     * @see HomekitAccessoryType
     * @see JsonObject
     */
    public HomekitPersistedAccessory(String accessoryType, String json) {
        this.accessoryType = accessoryType;
        this.json = json;
        logger.debug("{}Created new persisted accessory of type {}", LOG_INIT, accessoryType);
    }

    /**
     * Gets the type of this accessory.
     * The type is used to identify the correct factory for restoring the accessory.
     *
     * @return The accessory type
     * @see HomekitAccessoryType
     */
    public String getAccessoryType() {
        return accessoryType;
    }

    /**
     * Gets the JSON representation of this accessory.
     * The JSON contains all the information needed to restore the accessory's
     * state.
     *
     * @return The JSON representation
     * @see JsonObject
     */
    public String getJson() {
        return json;
    }

    /**
     * Updates the JSON data for this accessory.
     * This method is used to update the persisted state of the accessory.
     *
     * @param json The new JSON representation
     * @see JsonObject
     */
    public void setJson(String json) {
        this.json = json;
        logger.debug("{}Updated JSON data for accessory type {}", LOG_STATE, accessoryType);
    }

    /**
     * Updates the type of this accessory.
     * This method is used to change the accessory type if needed.
     *
     * @param accessoryType The new accessory type
     * @see HomekitAccessoryType
     */
    public void setAccessoryType(String accessoryType) {
        this.accessoryType = accessoryType;
        logger.debug("{}Updated accessory type to {}", LOG_STATE, accessoryType);
    }
}
