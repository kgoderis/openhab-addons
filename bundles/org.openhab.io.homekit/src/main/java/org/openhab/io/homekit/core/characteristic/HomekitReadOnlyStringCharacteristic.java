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

package org.openhab.io.homekit.core.characteristic;

import java.util.Map;

import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.types.StringType;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for HomeKit characteristics that handle read-only string values.
 *
 * <p>
 * This class extends {@link AbstractHomekitCharacteristic} to provide a specialized implementation
 * for string characteristics that can only be read from, not written to. It is particularly useful
 * for characteristics that represent static or read-only information, such as device names,
 * serial numbers, or firmware versions.
 * </p>
 *
 * <p>
 * The class integrates with several key components:
 * </p>
 * <ul>
 * <li>{@link AbstractHomekitCharacteristic} for base characteristic functionality</li>
 * <li>{@link HomekitService} for service-level operations</li>
 * <li>{@link HomekitEventManager} for event handling</li>
 * <li>{@link org.openhab.core.types.State} for state conversion</li>
 * <li>{@link javax.json.JsonValue} for JSON serialization</li>
 * <li>{@link org.openhab.core.library.types.StringType} for string state handling</li>
 * </ul>
 *
 * <p>
 * Key features:
 * </p>
 * <ul>
 * <li>Read-only access control through permission management</li>
 * <li>String value conversion between HomeKit and OpenHAB formats</li>
 * <li>Event handling for value changes</li>
 * <li>JSON serialization for HomeKit protocol communication</li>
 * <li>Integration with OpenHAB's state management system</li>
 * <li>Support for string validation and constraints</li>
 * <li>One-time value setting capability - value can be set exactly once, then becomes read-only</li>
 * </ul>
 *
 * <p>
 * The class follows the HomeKit Accessory Protocol (HAP) specification for string characteristics
 * and integrates with OpenHAB's state management system for reliable device information display.
 * It provides a robust implementation for read-only string characteristics while ensuring proper
 * integration with both HomeKit and OpenHAB ecosystems.
 * </p>
 *
 * @author Karel Goderis - Initial contribution
 * @version 1.0
 * @since 1.0
 */
@NonNullByDefault
public abstract class HomekitReadOnlyStringCharacteristic extends AbstractHomekitCharacteristic<String> {
    // ========== Log Message Prefixes ==========
    protected static final String LOG_PREFIX = "Homekit ReadOnlyString: ";
    protected static final String LOG_CHAR = LOG_PREFIX + "Characteristic - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";

    private static final Logger logger = LoggerFactory.getLogger(HomekitReadOnlyStringCharacteristic.class);
    private static final int MAX_LEN = 255;

    // Flag to track if the value has been set once
    private boolean valueSet = false;

    /**
     * Creates a new read-only string characteristic.
     *
     * <p>
     * This constructor initializes a new read-only string characteristic with its required
     * dependencies and sets up the event subscription system. It integrates with:
     * </p>
     * <ul>
     * <li>{@link HomekitService} for service integration</li>
     * <li>{@link HomekitEventManager} for event handling</li>
     * </ul>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Validates input parameters</li>
     * <li>Sets up read-only permissions</li>
     * <li>Configures string format</li>
     * <li>Initializes default value</li>
     * <li>Provides trace-level logging</li>
     * </ul>
     *
     * @param service the service this characteristic belongs to
     * @param eventManager the event manager for handling events
     * @throws IllegalArgumentException if any required parameter is null
     * @since 1.0
     */
    public HomekitReadOnlyStringCharacteristic(HomekitService service, HomekitEventManager eventManager) {
        super(service, eventManager);
        withFormat("string").withPairedWrite(false).withPairedRead(true).withEvents(false);
        initializeValue();
        logger.trace("{}Created new read-only string characteristic for service: {}", LOG_CHAR, service);
    }

    /**
     * Creates a new read-only string characteristic from a JSON value.
     *
     * <p>
     * This constructor initializes a read-only string characteristic from a JSON configuration,
     * allowing for flexible characteristic creation and configuration. It integrates with:
     * </p>
     * <ul>
     * <li>{@link javax.json.JsonValue} for configuration parsing</li>
     * <li>{@link HomekitService} for service integration</li>
     * <li>{@link HomekitEventManager} for event handling</li>
     * </ul>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Validates JSON configuration</li>
     * <li>Sets up read-only permissions</li>
     * <li>Configures string format</li>
     * <li>Initializes default value</li>
     * <li>Provides trace-level logging</li>
     * </ul>
     *
     * @param service the service this characteristic belongs to
     * @param eventManager the event manager for handling events
     * @param value the JSON value containing characteristic configuration
     * @throws IllegalArgumentException if the JSON value is invalid or required parameters are null
     * @since 1.0
     */
    public HomekitReadOnlyStringCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
        initializeValue();
        logger.trace("{}Created new read-only string characteristic from JSON for service: {}", LOG_CHAR, service);
    }

    /**
     * Sets the value of this characteristic. This method can only be called once.
     * After the first call, the characteristic becomes read-only.
     *
     * <p>
     * This method overrides the base implementation to enforce the one-time setting behavior.
     * It integrates with:
     * </p>
     * <ul>
     * <li>{@link AbstractHomekitCharacteristic} for base value setting</li>
     * <li>{@link HomekitEventManager} for event handling</li>
     * </ul>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Checks if value has already been set</li>
     * <li>Allows first value setting</li>
     * <li>Makes characteristic read-only after first set</li>
     * <li>Provides trace-level logging</li>
     * </ul>
     *
     * @param value the new value to set
     * @throws IllegalStateException if the value has already been set
     * @since 1.0
     */
    @Override
    public void setValue(@Nullable String value) throws IllegalStateException {
        if (valueSet) {
            logger.warn("{}Attempting to set value on read-only characteristic that has already been set", LOG_ERROR);
            throw new IllegalStateException("Cannot modify a read-only characteristic that has already been set");
        }

        // Temporarily enable write permission for the first set
        withPairedWrite(true);

        try {
            super.setValue(value);
            valueSet = true;

            // Make it read-only after setting
            withPairedWrite(false);

            logger.trace("{}Value set successfully, characteristic is now read-only", LOG_CHAR);
        } catch (Exception e) {
            // Restore read-only state if setting failed
            withPairedWrite(false);
            throw e;
        }
    }

    /**
     * Sets the value of this characteristic with metadata. This method can only be called once.
     * After the first call, the characteristic becomes read-only.
     *
     * <p>
     * This method overrides the base implementation to enforce the one-time setting behavior.
     * It integrates with:
     * </p>
     * <ul>
     * <li>{@link javax.json.JsonValue} for value parsing</li>
     * <li>{@link AbstractHomekitCharacteristic} for base value setting</li>
     * <li>{@link HomekitEventManager} for event handling</li>
     * <li>{@link org.openhab.io.homekit.event.core.HomekitEventMetadata} for metadata handling</li>
     * </ul>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Checks if value has already been set</li>
     * <li>Allows first value setting</li>
     * <li>Makes characteristic read-only after first set</li>
     * <li>Provides trace-level logging</li>
     * </ul>
     *
     * @param value the JSON value to set
     * @param conversionMap a map of conversion rules for the value
     * @param metadata the event metadata
     * @throws IllegalStateException if the value has already been set
     * @since 1.0
     */
    @Override
    protected void setValue(JsonValue value, Map<String, Object> conversionMap,
            org.openhab.io.homekit.event.core.HomekitEventMetadata metadata) throws IllegalStateException {
        if (valueSet) {
            logger.warn("{}Attempting to set value with metadata on read-only characteristic that has already been set",
                    LOG_ERROR);
            throw new IllegalStateException("Cannot modify a read-only characteristic that has already been set");
        }

        // Temporarily enable write permission for the first set
        withPairedWrite(true);

        try {
            super.setValue(value, conversionMap, metadata);
            valueSet = true;

            // Make it read-only after setting
            withPairedWrite(false);

            logger.trace("{}Value with metadata set successfully, characteristic is now read-only", LOG_CHAR);
        } catch (Exception e) {
            // Restore read-only state if setting failed
            withPairedWrite(false);
            throw e;
        }
    }

    /**
     * Checks if the value has been set for this characteristic.
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Returns the value set flag</li>
     * <li>Used to determine if characteristic is still writable</li>
     * <li>Provides trace-level logging</li>
     * </ul>
     *
     * @return true if the value has been set, false otherwise
     * @since 1.0
     */
    public boolean isValueSet() {
        logger.trace("{}Checking if value has been set: {}", LOG_CHAR, valueSet);
        return valueSet;
    }

    /**
     * Indicates that this characteristic is not hidden in the HomeKit interface.
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Always returns false</li>
     * <li>Ensures visibility in HomeKit</li>
     * <li>Provides trace-level logging</li>
     * </ul>
     *
     * @return false, as read-only string characteristics are always visible
     */
    @Override
    public boolean isHidden() {
        logger.trace("{}Checking if characteristic is hidden: false", LOG_CHAR);
        return false;
    }

    /**
     * Gets the default value for this characteristic.
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Returns empty string</li>
     * <li>Used for initialization</li>
     * <li>Provides trace-level logging</li>
     * </ul>
     *
     * @return the default string value (empty string)
     * @since 1.0
     */
    @Override
    public String getDefault() {
        logger.trace("{}Getting default value: empty string", LOG_CHAR);
        return "";
    }

    /**
     * Converts a JSON value to a string value.
     *
     * <p>
     * This method handles the conversion of JSON values to string values, supporting
     * various JSON value types and conversion rules. It integrates with:
     * </p>
     * <ul>
     * <li>{@link javax.json.JsonValue} for value parsing</li>
     * <li>{@link javax.json.JsonObject} for object handling</li>
     * <li>{@link javax.json.JsonString} for string handling</li>
     * </ul>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Validates JSON value type</li>
     * <li>Extracts string value</li>
     * <li>Handles conversion errors</li>
     * <li>Provides trace-level logging</li>
     * </ul>
     *
     * @param jsonValue the JSON value to convert
     * @param conversionMap a map of conversion rules for the value
     * @return the converted string value
     * @throws IllegalArgumentException if the JSON value cannot be converted to a string
     * @since 1.0
     */
    @Override
    public String toValue(JsonValue jsonValue, Map<String, Object> conversionMap) {
        if (!(jsonValue instanceof JsonString)) {
            logger.error("{}Invalid JSON value type for string conversion: {}", LOG_ERROR, jsonValue.getValueType());
            throw new IllegalArgumentException("Invalid JSON value type for string conversion");
        }
        String result = ((JsonString) jsonValue).getString();
        logger.debug("{}Converted JSON value to string: {}", LOG_CHAR, result);
        return result;
    }

    /**
     * Converts the characteristic to a JSON object with metadata.
     * Includes maximum string length constraint.
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Adds max length constraint</li>
     * <li>Enriches base JSON</li>
     * <li>Provides trace-level logging</li>
     * </ul>
     *
     * @return the JSON representation of the characteristic
     */
    @Override
    public JsonObject toJson() {
        JsonObject base = super.toJson();
        JsonObject result = enrich(base, "maxLen", MAX_LEN);
        logger.debug("{}Converted characteristic to JSON with max length: {}", LOG_CHAR, MAX_LEN);
        return result;
    }

    /**
     * Converts the characteristic to a reduced JSON object.
     * Includes maximum string length constraint.
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Adds max length constraint</li>
     * <li>Enriches reduced JSON</li>
     * <li>Provides trace-level logging</li>
     * </ul>
     *
     * @return the reduced JSON representation of the characteristic
     */
    @Override
    public JsonObject toReducedJson() {
        JsonObject base = super.toReducedJson();
        JsonObject result = enrich(base, "maxLen", MAX_LEN);
        logger.debug("{}Converted characteristic to reduced JSON with max length: {}", LOG_CHAR, MAX_LEN);
        return result;
    }

    /**
     * Converts the characteristic to a JSON object with specified metadata.
     * Includes maximum string length constraint.
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Adds max length constraint</li>
     * <li>Enriches JSON with specified metadata</li>
     * <li>Provides trace-level logging</li>
     * </ul>
     *
     * @param includeMeta whether to include metadata
     * @param includePermissions whether to include permissions
     * @param includeType whether to include type information
     * @param includeEvent whether to include event information
     * @return the JSON representation of the characteristic
     */
    @Override
    public JsonObject toJson(boolean includeMeta, boolean includePermissions, boolean includeType,
            boolean includeEvent) {
        JsonObject base = super.toJson(includeMeta, includePermissions, includeType, includeEvent);
        JsonObject result = enrich(base, "maxLen", MAX_LEN);
        logger.debug("{}Converted characteristic to JSON with specified metadata and max length: {}", LOG_CHAR,
                MAX_LEN);
        return result;
    }

    /**
     * Converts a State to a string value.
     *
     * <p>
     * This method handles the conversion of OpenHAB states to string values, supporting
     * various state types and conversion rules. It integrates with:
     * </p>
     * <ul>
     * <li>{@link org.openhab.core.types.State} for state handling</li>
     * <li>{@link org.openhab.core.library.types.StringType} for string state handling</li>
     * <li>{@link org.openhab.core.library.types.DecimalType} for numeric state handling</li>
     * </ul>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Validates state type</li>
     * <li>Handles conversion errors</li>
     * <li>Returns default value if needed</li>
     * <li>Provides trace-level logging</li>
     * </ul>
     *
     * @param state the state to convert
     * @param conversionMap a map of conversion rules for the value
     * @return the converted string value
     * @throws IllegalArgumentException if the state cannot be converted to a string
     * @since 1.0
     */
    @Override
    public String toValue(State state, Map<String, Object> conversionMap) {
        StringType convertedState = state.as(StringType.class);
        if (convertedState == null) {
            logger.debug("{}State conversion failed, using default value", LOG_CHAR);
            return getDefault();
        }
        String result = convertedState.toFullString();
        logger.debug("{}Converted state to string: {}", LOG_CHAR, result);
        return result;
    }

    /**
     * Converts a string value to a State.
     *
     * <p>
     * This method handles the conversion of string values to OpenHAB states, supporting
     * various state types and conversion rules. It integrates with:
     * </p>
     * <ul>
     * <li>{@link org.openhab.core.types.State} for state creation</li>
     * <li>{@link org.openhab.core.library.types.StringType} for string state creation</li>
     * </ul>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Creates StringType state</li>
     * <li>Validates input value</li>
     * <li>Provides trace-level logging</li>
     * </ul>
     *
     * @param value the string value to convert
     * @return the converted state
     * @since 1.0
     */
    @Override
    public State toState(String value) {
        State result = StringType.valueOf(value);
        logger.debug("{}Converted string to state: {}", LOG_CHAR, result);
        return result;
    }

    /**
     * Gets the accepted OpenHAB item type for this characteristic.
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Returns string item type</li>
     * <li>Used for item validation</li>
     * <li>Provides trace-level logging</li>
     * </ul>
     *
     * @return the item type string for strings
     */
    public static String getAcceptedItemType() {
        logger.trace("{}Getting accepted item type: {}", LOG_CHAR, CoreItemFactory.STRING);
        return CoreItemFactory.STRING;
    }
}
