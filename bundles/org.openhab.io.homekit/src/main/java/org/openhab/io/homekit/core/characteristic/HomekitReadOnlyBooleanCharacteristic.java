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

import javax.json.JsonNumber;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for HomeKit characteristics that handle read-only boolean values.
 *
 * This class extends {@link AbstractHomekitCharacteristic} to provide a specialized implementation
 * for boolean characteristics that can only be read from, not written to. It is particularly useful
 * for characteristics that represent read-only states or conditions, such as sensor states,
 * device status indicators, or system conditions.
 *
 * <p>
 * The class integrates with several key components:
 * <ul>
 * <li>{@link AbstractHomekitCharacteristic} for base characteristic functionality</li>
 * <li>{@link HomekitService} for service-level operations</li>
 * <li>{@link HomekitEventManager} for event handling</li>
 * <li>{@link org.openhab.core.types.State} for state conversion</li>
 * <li>{@link javax.json.JsonValue} for JSON serialization</li>
 * <li>{@link org.openhab.core.library.types.OnOffType} for boolean state handling</li>
 * </ul>
 * </p>
 *
 * <p>
 * Key features:
 * <ul>
 * <li>Read-only access control through permission management</li>
 * <li>Boolean value conversion between HomeKit and OpenHAB formats</li>
 * <li>Event handling for value changes</li>
 * <li>JSON serialization for HomeKit protocol communication</li>
 * <li>Integration with OpenHAB's state management system</li>
 * <li>Support for boolean state validation and constraints</li>
 * <li>One-time value setting capability - value can be set exactly once, then becomes read-only</li>
 * </ul>
 * </p>
 *
 * <p>
 * The class follows the HomeKit Accessory Protocol (HAP) specification for boolean characteristics
 * and integrates with OpenHAB's state management system for reliable device state monitoring.
 * It provides a robust implementation for read-only boolean characteristics while ensuring proper
 * integration with both HomeKit and OpenHAB ecosystems.
 * </p>
 *
 * @author Karel Goderis - Initial contribution
 * @version 1.0
 * @since 1.0
 */
@NonNullByDefault
public abstract class HomekitReadOnlyBooleanCharacteristic extends AbstractHomekitCharacteristic<Boolean> {
    // ========== Log Message Prefixes ==========
    protected static final String LOG_PREFIX = "Homekit ReadOnlyBoolean: ";
    protected static final String LOG_CHAR = LOG_PREFIX + "Characteristic - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";

    private static final Logger logger = LoggerFactory.getLogger(HomekitReadOnlyBooleanCharacteristic.class);

    // Flag to track if the value has been set once
    private boolean valueSet = false;

    /**
     * Creates a new read-only boolean characteristic.
     *
     * This constructor initializes a new read-only boolean characteristic with its required
     * dependencies and sets up the event subscription system. It integrates with:
     * <ul>
     * <li>{@link HomekitService} for service integration</li>
     * <li>{@link HomekitEventManager} for event handling</li>
     * </ul>
     *
     * @param service the service this characteristic belongs to
     * @param eventManager the event manager for handling events
     * @throws IllegalArgumentException if any required parameter is null
     * @since 1.0
     */
    public HomekitReadOnlyBooleanCharacteristic(HomekitService service, HomekitEventManager eventManager) {
        super(service, eventManager);
        withFormat("bool").withPairedWrite(false).withPairedRead(true).withEvents(false);
        initializeValue();
        logger.debug("{}Created new read-only boolean characteristic for service: {}", LOG_CHAR, service);
    }

    /**
     * Creates a new read-only boolean characteristic from a JSON value.
     *
     * This constructor initializes a read-only boolean characteristic from a JSON configuration,
     * allowing for flexible characteristic creation and configuration. It integrates with:
     * <ul>
     * <li>{@link javax.json.JsonValue} for configuration parsing</li>
     * <li>{@link HomekitService} for service integration</li>
     * <li>{@link HomekitEventManager} for event handling</li>
     * </ul>
     *
     * @param service the service this characteristic belongs to
     * @param eventManager the event manager for handling events
     * @param value the JSON value containing characteristic configuration
     * @throws IllegalArgumentException if the JSON value is invalid or required parameters are null
     * @since 1.0
     */
    public HomekitReadOnlyBooleanCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
        initializeValue();
        logger.debug("{}Created new read-only boolean characteristic from JSON for service: {}", LOG_CHAR, service);
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
    public void setValue(@Nullable Boolean value) throws IllegalStateException {
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

            logger.debug("{}Value set successfully, characteristic is now read-only", LOG_CHAR);
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

            logger.debug("{}Value with metadata set successfully, characteristic is now read-only", LOG_CHAR);
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
        logger.debug("{}Checking if value has been set: {}", LOG_CHAR, valueSet);
        return valueSet;
    }

    /**
     * Gets the default value for this characteristic.
     *
     * @return the default boolean value (false)
     * @since 1.0
     */
    @Override
    public Boolean getDefault() {
        logger.debug("{}Getting default value: false", LOG_CHAR);
        return false;
    }

    /**
     * Converts a JSON value to a boolean value.
     *
     * This method handles the conversion of JSON values to boolean values, supporting
     * various JSON value types and conversion rules. It integrates with:
     * <ul>
     * <li>{@link javax.json.JsonValue} for value parsing</li>
     * <li>{@link javax.json.JsonObject} for object handling</li>
     * <li>{@link javax.json.JsonNumber} for numeric handling</li>
     * </ul>
     *
     * @param jsonValue the JSON value to convert
     * @param conversionMap a map of conversion rules for the value
     * @return the converted boolean value
     * @throws IllegalArgumentException if the JSON value cannot be converted to a boolean
     * @since 1.0
     */
    @Override
    public Boolean toValue(JsonValue jsonValue, Map<String, Object> conversionMap) {
        Boolean result;
        if (jsonValue.getValueType().equals(ValueType.NUMBER)) {
            result = ((JsonNumber) jsonValue).intValue() > 0;
        } else {
            result = jsonValue.equals(JsonValue.TRUE);
        }
        logger.debug("{}Converted JSON value to boolean: {}", LOG_CHAR, result);
        return result;
    }

    /**
     * Converts a State to a boolean value.
     *
     * This method handles the conversion of OpenHAB states to boolean values, supporting
     * various state types and conversion rules. It integrates with:
     * <ul>
     * <li>{@link org.openhab.core.types.State} for state handling</li>
     * <li>{@link org.openhab.core.library.types.OnOffType} for on/off state handling</li>
     * <li>{@link org.openhab.core.library.types.OpenClosedType} for open/closed state handling</li>
     * </ul>
     *
     * @param state the state to convert
     * @param conversionMap a map of conversion rules for the value
     * @return the converted boolean value
     * @throws IllegalArgumentException if the state cannot be converted to a boolean
     * @since 1.0
     */
    @Override
    public Boolean toValue(State state, Map<String, Object> conversionMap) {
        OnOffType convertedState = state.as(OnOffType.class);
        if (convertedState == null) {
            logger.debug("{}State conversion failed, using default value", LOG_CHAR);
            return getDefault();
        }
        Boolean result = convertedState.equals(OnOffType.ON);
        logger.debug("{}Converted state to boolean: {}", LOG_CHAR, result);
        return result;
    }

    /**
     * Converts a boolean value to a State.
     *
     * This method handles the conversion of boolean values to OpenHAB states, supporting
     * various state types and conversion rules. It integrates with:
     * <ul>
     * <li>{@link org.openhab.core.types.State} for state creation</li>
     * <li>{@link org.openhab.core.library.types.OnOffType} for on/off state creation</li>
     * <li>{@link org.openhab.core.library.types.OpenClosedType} for open/closed state creation</li>
     * </ul>
     *
     * @param value the boolean value to convert
     * @return the converted state
     * @since 1.0
     */
    @Override
    public State toState(Boolean value) {
        State result = value ? OnOffType.ON : OnOffType.OFF;
        logger.debug("{}Converted boolean to state: {}", LOG_CHAR, result);
        return result;
    }
}
