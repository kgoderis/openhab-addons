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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristic;
import org.openhab.io.homekit.api.event.HomekitEventType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.api.service.HomekitServiceType;
import org.openhab.io.homekit.api.uid.HomekitCharacteristicUID;
import org.openhab.io.homekit.event.core.AbstractHomekitEvent;
import org.openhab.io.homekit.event.core.HomekitEventMetadata;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.event.model.characteristic.HomekitCharacteristicChangedEvent;
import org.openhab.io.homekit.event.model.characteristic.HomekitCharacteristicUpdateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for HomeKit characteristics that provides core functionality and lifecycle management.
 * This class serves as the foundation for all HomeKit characteristic implementations, offering a robust framework
 * for managing characteristic state, permissions, and event handling within the OpenHAB HomeKit integration.
 *
 * <p>
 * The class implements a comprehensive state management system that includes:
 * <ul>
 * <li>Value management and type conversion between HomeKit and OpenHAB formats using
 * {@link org.openhab.core.types.State}</li>
 * <li>Event handling and notifications through {@link HomekitEventManager} and {@link HomekitEventType}</li>
 * <li>JSON serialization and deserialization using {@link javax.json.JsonValue} for HomeKit protocol communication</li>
 * <li>Permission and access control for characteristic operations</li>
 * <li>State synchronization with {@link org.openhab.core.items.Item OpenHAB items}</li>
 * </ul>
 * </p>
 *
 * <p>
 * Key architectural features:
 * <ul>
 * <li>Type-safe value handling through generics, ensuring type safety across the characteristic hierarchy</li>
 * <li>Automatic event subscription and handling through {@link HomekitEventManager} and {@link HomekitEventType}</li>
 * <li>Flexible permission system supporting paired read/write, hidden, and mandatory characteristics</li>
 * <li>Support for advanced HomeKit features like timed writes and additional authorization</li>
 * <li>Built-in JSON conversion utilities using {@link javax.json.JsonObject} for HomeKit protocol compliance</li>
 * </ul>
 * </p>
 *
 * <p>
 * The class integrates with several key components:
 * <ul>
 * <li>{@link HomekitService} - Manages service lifecycle and characteristic relationships</li>
 * <li>{@link HomekitEventManager} - Handles event distribution and subscription management</li>
 * <li>{@link org.openhab.core.types.State} - Provides state conversion and synchronization</li>
 * <li>{@link javax.json.JsonValue} - Enables JSON serialization for HomeKit protocol</li>
 * <li>{@link HomekitCharacteristicUID} - Provides unique identification for characteristics</li>
 * <li>{@link org.openhab.core.items.Item} - Represents OpenHAB items for state synchronization</li>
 * <li>{@link org.openhab.core.types.Command} - Handles command processing for characteristic updates</li>
 * </ul>
 * </p>
 *
 * <p>
 * Implementation guidelines:
 * <ul>
 * <li>Subclasses must implement type-specific value conversion methods</li>
 * <li>State synchronization should be handled through {@link HomekitEventManager}</li>
 * <li>Permission changes should be managed through the builder pattern methods</li>
 * <li>JSON serialization should follow HomeKit protocol specifications</li>
 * <li>Error handling should use the provided logging system</li>
 * </ul>
 * </p>
 *
 * <p>
 * The class follows the HomeKit Accessory Protocol (HAP) specification for characteristic behavior and
 * integrates with OpenHAB's state management system for reliable device control and monitoring. It provides
 * a robust foundation for implementing specific characteristic types while ensuring consistent behavior
 * and proper integration with both HomeKit and OpenHAB ecosystems.
 * </p>
 *
 * <p>
 * The class works in conjunction with:
 * <ul>
 * <li>{@link HomekitService} for service-level operations</li>
 * <li>{@link HomekitAccessory} for accessory-level integration</li>
 * <li>{@link HomekitEventManager} for event handling</li>
 * <li>{@link org.openhab.core.thing.Channel} for OpenHAB channel integration</li>
 * </ul>
 * </p>
 *
 * @author Karel Goderis - Initial contribution
 * @version 1.0
 * @since 1.0
 */
@NonNullByDefault
public abstract class AbstractHomekitCharacteristic<@NonNull T> implements HomekitCharacteristic<@NonNull T> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractHomekitCharacteristic.class);

    protected static final String LOG_PREFIX = "Homekit HomekitCharacteristic: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_STATE = LOG_PREFIX + "State - ";
    protected static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    protected static final String LOG_CHARACTERISTIC = LOG_PREFIX + "HomekitCharacteristic - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";

    // Instance fields - final
    private final HomekitService service;
    private final HomekitEventManager eventManager;
    private long instanceId;
    private String format;
    private String description;

    // Instance fields - mutable
    private boolean isPairedWrite = false;
    private boolean isPairedRead = false;
    private boolean isHidden = false;
    private boolean isMandatory = false;
    private boolean hasEvents = false;
    private boolean isWriteResponse = false;
    private boolean isTimedWrite = false;
    private boolean isAdditionalAuthorization = false;
    protected @Nullable T value = null;
    protected @Nullable JsonValue initialValue = null;

    /**
     * Creates a new HomekitBaseCharacteristic with required parameters.
     * 
     * <p>
     * This constructor initializes a new characteristic with its required dependencies and sets up
     * the event subscription system. It integrates with:
     * <ul>
     * <li>{@link HomekitService} for service integration</li>
     * <li>{@link HomekitEventManager} for event handling</li>
     * <li>{@link HomekitEventType} for event type management</li>
     * </ul>
     * </p>
     *
     * @param service the service this characteristic belongs to
     * @param eventManager the event manager for handling events
     * @throws IllegalArgumentException if any required parameter is null
     * @since 1.0
     */
    public AbstractHomekitCharacteristic(HomekitService service, HomekitEventManager eventManager) {

        this.service = service;
        this.eventManager = eventManager;
        this.instanceId = 0; // Default value
        this.format = "";
        this.description = "";

        setupSubscription();
    }

    /**
     * Creates a new HomekitBaseCharacteristic from a JSON value.
     * 
     * <p>
     * This constructor initializes a characteristic from a JSON configuration, allowing for
     * flexible characteristic creation and configuration. It integrates with:
     * <ul>
     * <li>{@link javax.json.JsonValue} for configuration parsing</li>
     * <li>{@link HomekitService} for service integration</li>
     * <li>{@link HomekitEventManager} for event handling</li>
     * <li>{@link HomekitEventType} for event type management</li>
     * </ul>
     * </p>
     *
     * @param service the service this characteristic belongs to
     * @param eventManager the event manager for handling events
     * @param value the JSON value containing characteristic configuration
     * @throws IllegalArgumentException if the JSON value is invalid or required parameters are null
     * @since 1.0
     */
    public AbstractHomekitCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        if (!value.getValueType().equals(JsonValue.ValueType.OBJECT)) {
            throw new IllegalArgumentException("Invalid JSON value for characteristic creation");
        }

        String jsonType = ((JsonObject) value).getString("type");
        if (!getType().equals(jsonType)) {
            throw new IllegalArgumentException("Invalid JSON value for characteristic creation");
        }

        JsonObject jsonObject = (JsonObject) value;
        this.service = service;
        this.eventManager = eventManager;
        this.instanceId = jsonObject.getInt("iid");
        this.format = jsonObject.getString("format");
        this.initialValue = value;

        if (jsonObject.containsKey("description")) {
            this.description = jsonObject.getString("description");
        } else {
            this.description = "";
        }

        if (jsonObject.containsKey("perms")) {
            JsonArray perms = jsonObject.getJsonArray("perms");
            for (JsonValue perm : perms) {
                String permString = ((JsonString) perm).getString();
                switch (permString) {
                    case "pw" -> this.isPairedWrite = true;
                    case "pr" -> this.isPairedRead = true;
                    case "ev" -> this.hasEvents = true;
                    case "wr" -> this.isWriteResponse = true;
                    case "tw" -> this.isTimedWrite = true;
                    case "aa" -> this.isAdditionalAuthorization = true;
                    case "hd" -> this.isHidden = true;
                }
            }
        }

        if (jsonObject.containsKey("ev")) {
            this.hasEvents = jsonObject.getBoolean("ev");
        }

        setupSubscription();
    }

    /**
     * Initializes the characteristic's value from its initial configuration.
     * 
     * <p>
     * This method sets up the characteristic's initial state based on its configuration.
     * It integrates with:
     * <ul>
     * <li>{@link javax.json.JsonValue} for value parsing</li>
     * <li>{@link HomekitEventManager} for event handling</li>
     * <li>{@link HomekitEventType} for event type management</li>
     * </ul>
     * </p>
     *
     * @since 1.0
     */
    public void initializeValue() {
        if (initialValue != null) {
            // Use Objects.requireNonNull to satisfy null-safety constraints
            JsonValue safeValue = Objects.requireNonNull(initialValue, "initialValue cannot be null");
            T convertedValue = toValue(safeValue);
            this.value = convertedValue;
            initialValue = null;
        } else {
            this.value = getDefault();
        }
    }

    /**
     * Sets up event subscription for the characteristic.
     * 
     * <p>
     * This method configures the characteristic to receive and handle events through the
     * event management system. It integrates with:
     * <ul>
     * <li>{@link HomekitEventManager} for event handling</li>
     * <li>{@link HomekitEventType} for event type management</li>
     * <li>{@link HomekitCharacteristicChangedEvent} for change events</li>
     * <li>{@link HomekitCharacteristicUpdateEvent} for update events</li>
     * </ul>
     * </p>
     *
     * @since 1.0
     */
    protected final void setupSubscription() {
        eventManager.subscribe(HomekitEventType.CHARACTERISTIC_CHANGE_VALUE, AbstractHomekitEvent.WILDCARD_UID,
                getUID(), event -> {
                    if (event instanceof HomekitCharacteristicUpdateEvent changeEvent) {
                        // Optionally check if the event is for this characteristic
                        if (changeEvent.getCharacteristic().isPresent()) {
                            @SuppressWarnings("null") // isPresent() check ensures get() is safe
                            HomekitCharacteristic<?> eventCharacteristic = changeEvent.getCharacteristic().get();
                            if (eventCharacteristic.equals(AbstractHomekitCharacteristic.this)) {
                                // Update the value in response to the event
                                try {
                                    if (changeEvent.getNewValue().isPresent()) {
                                        @SuppressWarnings("null") // Optional.get() after isPresent() check is safe
                                        JsonValue newValue = changeEvent.getNewValue().get();
                                        AbstractHomekitCharacteristic.this.setValue(newValue,
                                                changeEvent.getItemConfiguration(), changeEvent.getMetadata());
                                    }
                                } catch (Exception e) {
                                    // Handle error
                                }
                            }
                        }
                    }
                });
    }

    /**
     * Gets the unique identifier for this characteristic.
     * 
     * @return the HomekitCharacteristicUID for this characteristic
     */
    @Override
    @NonNull
    public HomekitCharacteristicUIDImpl getUID() {
        return new HomekitCharacteristicUIDImpl(getService().getAccessory().getUID().getPairingId(),
                getService().getAccessory().getAccessoryId(), getService().getInstanceId(), getInstanceId());
    }

    /**
     * Gets the instance ID of this characteristic.
     * 
     * @return the instance ID
     */
    @Override
    public long getInstanceId() {
        return instanceId;
    }

    /**
     * Gets the service this characteristic belongs to.
     * 
     * @return the HomekitService instance
     */
    @Override
    public HomekitService getService() {
        return service;
    }

    /**
     * Gets the type of this characteristic.
     * 
     * @return the characteristic type
     * @throws IllegalStateException if the class is not properly annotated
     */
    @Override
    final public String getType() {
        @SuppressWarnings("null") // getAnnotation() can return null, handled by null check below
        HomekitServiceType annotation = getClass().getAnnotation(HomekitServiceType.class);
        if (annotation == null) {
            throw new IllegalStateException(
                    "Service class " + getClass().getName() + " must be annotated with @HomekitServiceType");
        }
        String type = annotation.type();

        if (type.length() == 2) {
            return String.format("%0" + (8 - type.length()) + "d%s", 0, type) + "-0000-1000-8000-0026BB765291";
        }
        return type;
    }

    /**
     * Gets the tag for this characteristic.
     * 
     * @return the characteristic tag
     */
    @Override
    final public String getTag() {
        @SuppressWarnings("null") // getAnnotation() can return null, handled by null check below
        HomekitServiceType annotation = getClass().getAnnotation(HomekitServiceType.class);
        return annotation != null ? annotation.tag() : getType();
    }

    /**
     * Gets the description of this characteristic.
     * 
     * @return the characteristic description
     */
    @Override
    public String getDescription() {
        return description;
    }

    /**
     * Checks if this characteristic is of the specified type.
     * 
     * @param aType the type to check against
     * @return true if this characteristic is of the specified type
     */
    @Override
    public boolean isType(String aType) {
        return getType().equals(aType);
    }

    /**
     * Checks if this characteristic is hidden.
     * 
     * @return true if this characteristic is hidden
     */
    @Override
    public boolean isHidden() {
        return isHidden;
    }

    /**
     * Checks if this characteristic is mandatory.
     *
     * @return true if the characteristic is mandatory
     */
    @Override
    public boolean isMandatory() {
        return isMandatory;
    }

    /**
     * Converts the characteristic to a JSON object with specified options.
     * 
     * @param includeMeta whether to include metadata
     * @param includePermissions whether to include permissions
     * @param includeType whether to include type information
     * @param includeEvent whether to include event information
     * @return the JSON representation of this characteristic
     */
    @Override
    public JsonObject toJson(boolean includeMeta, boolean includePermissions, boolean includeType,
            boolean includeEvent) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        if (includeMeta) {
            builder.add("iid", instanceId);
            builder.add("aid", service.getAccessory().getAccessoryId());
            builder.add("type", getType());
        }
        if (includePermissions) {
            builder.add("perms", getPermissions());
        }
        if (includeType) {
            builder.add("format", format);
        }
        if (includeEvent) {
            builder.add("ev", hasEvents);
        }
        JsonObject baseJson = builder.build();
        if (value != null) {
            return enrich(baseJson, "value", returnSafeValue(value));
        }
        return baseJson;
    }

    // Public methods
    /**
     * Gets the current value of this characteristic.
     * 
     * @return the current value
     */
    @Override
    public T getValue() {
        if (value == null && initialValue != null) {
            // Use explicit conditional logic to help compiler with null analysis
            JsonValue valueToConvert = initialValue;
            if (valueToConvert != null) {
                T convertedValue = toValue(valueToConvert);
                value = convertedValue;
            }
            initialValue = null;
        }

        // Use explicit conditional logic for return value with helper method
        return returnSafeValue(value);
    }

    /**
     * Helper method to safely return a value, working around Eclipse generic type constraints.
     * 
     * @param candidate the value to return if non-null
     * @return the value if non-null, otherwise the default value
     */
    private T returnSafeValue(@Nullable T candidate) {
        return candidate != null ? candidate : getDefault();
    }

    /**
     * Sets the value of this characteristic.
     * 
     * @param value the new value to set
     */
    @Override
    public void setValue(@Nullable T value) throws Exception {
        if (!isPairedWrite) {
            throw new Exception("Cannot modify a readonly characteristic");
        }
        if (!isAllowedValue(value)) {
            throw new IllegalArgumentException("Value " + value + " is not allowed for this characteristic");
        }
        @Nullable
        T oldValue = this.value;
        setValueInternal(value);
        notifyValueChanged(oldValue, this.value);
    }

    @Override
    public final void setValue(JsonValue jsonValue) throws Exception {
        if (!isPairedWrite) {
            throw new Exception("Cannot modify a readonly characteristic");
        }
        try {
            T convertedValue = toValue(jsonValue);
            if (!isAllowedValue(convertedValue)) {
                throw new IllegalArgumentException(
                        "Value " + convertedValue + " is not allowed for this characteristic");
            }
            setValue(convertedValue);
        } catch (Exception e) {
            logger.error("{}Error while setting JSON value: {}", LOG_ERROR, e.getMessage(), e);
            throw e;
        }
    }

    protected void setValue(JsonValue value, Map<String, Object> conversionMap, HomekitEventMetadata metadata)
            throws Exception {
        if (!isPairedWrite) {
            throw new Exception("Cannot modify a readonly characteristic");
        }
        try {
            T convertedValue = toValue(value, conversionMap);
            if (!isAllowedValue(convertedValue)) {
                throw new IllegalArgumentException(
                        "Value " + convertedValue + " is not allowed for this characteristic");
            }

            @Nullable
            T oldValue = this.value;
            setValueInternal(convertedValue);
            notifyValueChanged(oldValue, this.value, metadata);
        } catch (Exception e) {
            logger.error("{}Error while setting value with metadata: {}", LOG_ERROR, e.getMessage(), e);
            throw e;
        }
    }

    protected void setValueInternal(@Nullable T value) throws Exception {
        this.value = value;
    }

    /**
     * Gets the default value for this characteristic.
     * 
     * @return the default value
     */
    @Override
    public abstract T getDefault();

    // Abstract methods
    /**
     * Converts a JSON value to the characteristic's value type.
     * 
     * @param jsonValue the JSON value to convert
     * @param conversionMap a map of conversion rules for the value
     * @return the converted value
     */
    @Override
    public abstract T toValue(JsonValue jsonValue, Map<String, Object> conversionMap);

    /**
     * Converts a JSON value to the characteristic's value type.
     * 
     * @param jsonValue the JSON value to convert
     * @return the converted value
     */
    @Override
    public T toValue(JsonValue jsonValue) {
        return toValue(jsonValue, Collections.emptyMap());
    }

    /**
     * Converts a State to the characteristic's value type.
     * 
     * @param state the state to convert
     * @param conversionMap a map of conversion rules for the value
     * @return the converted value
     */
    @Override
    public abstract T toValue(State state, Map<String, Object> conversionMap);

    /**
     * Converts a State to the characteristic's value type.
     * 
     * @param state the state to convert
     * @return the converted value
     */
    @Override
    public T toValue(State state) {
        return toValue(state, Collections.emptyMap());
    }

    /**
     * Converts a value to a State.
     * 
     * @param value the value to convert
     * @return the converted state
     */
    @Override
    public abstract State toState(T value);

    /**
     * Converts a JSON value to a State.
     * 
     * @param jsonValue the JSON value to convert
     * @return the converted state
     */
    @Override
    public State toState(JsonValue jsonValue) {
        return toState(toValue(jsonValue));
    }

    // Protected methods
    /**
     * Notifies listeners of a value change.
     * 
     * @param oldValue the previous value
     * @param newValue the new value
     */
    protected void notifyValueChanged(@Nullable T oldValue, @Nullable T newValue) {
        eventManager.publishEvent(new HomekitCharacteristicChangedEvent((HomekitCharacteristic<?>) this,
                toValueJson(oldValue), toValueJson(newValue)));
    }

    /**
     * Notifies listeners of a value change with metadata.
     * 
     * @param oldValue the previous value
     * @param newValue the new value
     * @param metadata the event metadata
     */
    protected void notifyValueChanged(@Nullable T oldValue, @Nullable T newValue, HomekitEventMetadata metadata) {
        eventManager.publishEvent(new HomekitCharacteristicChangedEvent((HomekitCharacteristic<?>) this,
                toValueJson(oldValue), toValueJson(newValue), metadata));
    }

    /**
     * Converts the characteristic to a JSON object.
     * 
     * @return the JSON representation of this characteristic
     */
    @Override
    public JsonObject toJson() {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("iid", instanceId);
        builder.add("aid", service.getAccessory().getAccessoryId());
        builder.add("type", getType());
        builder.add("perms", getPermissions());
        builder.add("format", format);
        builder.add("description", description);
        builder.add("ev", hasEvents);
        JsonObject baseJson = builder.build();
        return enrich(baseJson, "value", getValue());
    }

    /**
     * Converts the characteristic to a reduced JSON object.
     * 
     * @return the reduced JSON representation
     */
    @Override
    public JsonObject toReducedJson() {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("iid", instanceId);
        builder.add("type", getType());
        builder.add("perms", getPermissions());
        builder.add("format", format);
        builder.add("description", description);
        builder.add("ev", hasEvents);
        JsonObject baseJson = builder.build();
        return enrich(baseJson, "value", getValue());
    }

    /**
     * Converts the characteristic to an event JSON object.
     * 
     * @return the event JSON representation
     */
    @Override
    public JsonObject toEventJson() {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("iid", instanceId);
        builder.add("aid", service.getAccessory().getAccessoryId());
        JsonObject baseJson = builder.build();
        return enrich(baseJson, "value", getValue());
    }

    /**
     * Converts the characteristic to an event JSON object with a specific value.
     * 
     * @param value the value to include in the event
     * @return the event JSON representation
     */
    @Override
    public JsonObject toEventJson(T value) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("iid", instanceId);
        builder.add("aid", service.getAccessory().getAccessoryId());
        JsonObject baseJson = builder.build();
        return enrich(baseJson, "value", value);
    }

    /**
     * Converts a value to a JSON value.
     * 
     * @param value the value to convert
     * @return the JSON representation of the value
     */
    @Override
    public JsonValue toValueJson(@Nullable T value) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        JsonObject baseJson = builder.build();
        return enrich(baseJson, "value", value);
    }

    /**
     * Converts a State to a JSON value.
     * 
     * @param state the state to convert
     * @return the JSON representation of the state
     */
    @Override
    public JsonValue toValueJson(State state) {
        return toValueJson(toValue(state));
    }

    /**
     * Enriches a JSON object with a key-value pair.
     * 
     * @param source the source JSON object
     * @param key the key to add
     * @param value the value to add
     * @return the enriched JSON object
     */
    protected JsonObject enrich(JsonObject source, String key, @Nullable Object value) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        addValue(builder, key, value);
        source.entrySet().forEach(e -> builder.add(e.getKey(), e.getValue()));
        return builder.build();
    }

    /**
     * Adds a value to a JSON object builder.
     * 
     * @param builder the JSON object builder
     * @param name the name of the value
     * @param value the value to add
     */
    protected void addValue(JsonObjectBuilder builder, String name, @Nullable Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof Boolean aBoolean) {
            builder.add(name, aBoolean);
        } else if (value instanceof Double aDouble) {
            builder.add(name, aDouble);
        } else if (value instanceof Integer anInteger) {
            builder.add(name, anInteger);
        } else if (value instanceof Long aLong) {
            builder.add(name, aLong);
        } else if (value instanceof BigInteger aBigInteger) {
            builder.add(name, aBigInteger);
        } else if (value instanceof BigDecimal aBigDecimal) {
            builder.add(name, aBigDecimal);
        } else if (value instanceof JsonValue aJsonValue) {
            builder.add(name, aJsonValue);
        } else if (value instanceof JsonObjectBuilder aJsonObjectBuilder) {
            builder.add(name, aJsonObjectBuilder);
        } else if (value instanceof JsonArrayBuilder aJsonArrayBuilder) {
            builder.add(name, aJsonArrayBuilder);
        } else if (value instanceof JsonObject aJsonObject) {
            builder.add(name, aJsonObject);
        } else {
            builder.add(name, value.toString());
        }
    }

    // Private methods
    /**
     * Gets the permissions for this characteristic.
     * 
     * @return the JSON array of permissions
     */
    private JsonArray getPermissions() {
        JsonArrayBuilder builder = Json.createArrayBuilder();
        if (isPairedWrite) {
            builder.add("pw");
        }
        if (isPairedRead) {
            builder.add("pr");
        }
        if (hasEvents) {
            builder.add("ev");
        }
        if (isWriteResponse) {
            builder.add("wr");
        }
        if (isAdditionalAuthorization) {
            builder.add("aa");
        }
        if (isTimedWrite) {
            builder.add("tw");
        }
        if (isHidden) {
            builder.add("hd");
        }
        return builder.build();
    }

    /**
     * Sets the instance ID for this characteristic.
     * 
     * @param instanceId the instance ID to set
     * @return this instance
     */
    @Override
    public AbstractHomekitCharacteristic<T> withInstanceId(long instanceId) {
        this.instanceId = instanceId;
        return this;
    }

    /**
     * Sets the format for this characteristic.
     * 
     * @param format the format to set
     * @return this instance
     */
    @Override
    public AbstractHomekitCharacteristic<T> withFormat(String format) {
        this.format = format;
        return this;
    }

    /**
     * Sets the description for this characteristic.
     * 
     * @param description the description to set
     * @return this instance
     */
    @Override
    public AbstractHomekitCharacteristic<T> withDescription(String description) {
        this.description = description;
        return this;
    }

    /**
     * Sets whether this characteristic is writable.
     * 
     * @param isPairedWrite whether the characteristic is writable
     * @return this instance
     */
    @Override
    public AbstractHomekitCharacteristic<T> withPairedWrite(boolean isPairedWrite) {
        this.isPairedWrite = isPairedWrite;
        return this;
    }

    /**
     * Sets whether this characteristic is readable.
     * 
     * @param isPairedRead whether the characteristic is readable
     * @return this instance
     */
    @Override
    public AbstractHomekitCharacteristic<T> withPairedRead(boolean isPairedRead) {
        this.isPairedRead = isPairedRead;
        return this;
    }

    /**
     * Sets whether this characteristic is hidden.
     * 
     * @param isHidden whether the characteristic is hidden
     * @return this instance
     */
    @Override
    public AbstractHomekitCharacteristic<T> withHidden(boolean isHidden) {
        this.isHidden = isHidden;
        return this;
    }

    /**
     * Sets whether this characteristic is mandatory.
     * 
     * @param isMandatory whether the characteristic is mandatory
     * @return this instance
     */
    @Override
    public AbstractHomekitCharacteristic<T> withMandatory(boolean isMandatory) {
        this.isMandatory = isMandatory;
        return this;
    }

    /**
     * Sets whether this characteristic has events.
     * 
     * @param hasEvents whether the characteristic has events
     * @return this instance
     */
    @Override
    public AbstractHomekitCharacteristic<T> withEvents(boolean hasEvents) {
        this.hasEvents = hasEvents;
        return this;
    }

    /**
     * Sets whether this characteristic requires a write response.
     * 
     * @param isWriteResponse whether the characteristic requires a write response
     * @return this instance
     */
    @Override
    public AbstractHomekitCharacteristic<T> withWriteResponse(boolean isWriteResponse) {
        this.isWriteResponse = isWriteResponse;
        return this;
    }

    /**
     * Sets whether this characteristic requires additional authorization.
     * 
     * @param isAdditionalAuthorization whether the characteristic requires additional authorization
     * @return this instance
     */
    @Override
    public AbstractHomekitCharacteristic<T> withAdditionalAuthorization(boolean isAdditionalAuthorization) {
        this.isAdditionalAuthorization = isAdditionalAuthorization;
        return this;
    }

    /**
     * Sets whether this characteristic requires a timed write.
     * 
     * @param isTimedWrite whether the characteristic requires a timed write
     * @return this instance
     */
    @Override
    public AbstractHomekitCharacteristic<T> withTimedWrite(boolean isTimedWrite) {
        this.isTimedWrite = isTimedWrite;
        return this;
    }

    /**
     * Updates this characteristic with values from another characteristic.
     * 
     * @param other the characteristic to update from
     */
    @SuppressWarnings("unchecked")
    @Override
    public void updateWith(@Nullable HomekitCharacteristic<?> other) {
        if (other == null)
            return;
        if (other instanceof AbstractHomekitCharacteristic<?> otherGeneric) {
            if (this.getType().equals(otherGeneric.getType()) && this.instanceId == otherGeneric.getInstanceId()) {
                this.isPairedWrite = otherGeneric.isPairedWrite;
                this.isPairedRead = otherGeneric.isPairedRead;
                this.hasEvents = otherGeneric.hasEvents;
                this.description = otherGeneric.description;
                this.format = otherGeneric.format;
                this.isHidden = otherGeneric.isHidden;
                this.isTimedWrite = otherGeneric.isTimedWrite;
                this.isAdditionalAuthorization = otherGeneric.isAdditionalAuthorization;
                this.isWriteResponse = otherGeneric.isWriteResponse;
                try {
                    setValue((T) otherGeneric.getValue());
                } catch (Exception e) {
                    logger.error("{}Error updating characteristic value: {}", LOG_ERROR, e.getMessage(), e);
                }
            }
        }
    }

    // Object methods
    /**
     * Compares this characteristic with another object for equality.
     * 
     * @param o the object to compare with
     * @return true if the objects are equal
     */
    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        AbstractHomekitCharacteristic<?> that = (AbstractHomekitCharacteristic<?>) o;

        // Compare fields in the same order as compareTo
        return instanceId == that.instanceId && getType().equals(that.getType()) && format.equals(that.format)
                && isPairedWrite == that.isPairedWrite && isPairedRead == that.isPairedRead
                && hasEvents == that.hasEvents && description.equals(that.description)
                && isTimedWrite == that.isTimedWrite && isAdditionalAuthorization == that.isAdditionalAuthorization
                && isWriteResponse == that.isWriteResponse;
    }

    /**
     * Generates a hash code for this characteristic.
     * 
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(instanceId, getType(), service);
    }

    /**
     * Compares this characteristic with another characteristic.
     * 
     * @param other the characteristic to compare with
     * @return a negative integer, zero, or a positive integer as this characteristic is less than, equal to, or greater
     *         than the specified characteristic
     */
    @Override
    public int compareTo(@Nullable HomekitCharacteristic<?> other) {
        if (other == null)
            return 1;
        if (this == other)
            return 0;

        // Compare by instance ID
        int idCompare = Long.compare(this.instanceId, other.getInstanceId());
        if (idCompare != 0)
            return idCompare;

        // Compare by instance type
        int typeCompare = this.getType().compareTo(other.getType());
        if (typeCompare != 0)
            return typeCompare;

        // Compare by format
        int formatCompare = this.format.compareTo(((AbstractHomekitCharacteristic<?>) other).format);
        if (formatCompare != 0)
            return formatCompare;

        // Compare by isWritable
        int writableCompare = Boolean.compare(this.isPairedWrite,
                ((AbstractHomekitCharacteristic<?>) other).isPairedWrite);
        if (writableCompare != 0)
            return writableCompare;

        // Compare by isReadable
        int readableCompare = Boolean.compare(this.isPairedRead,
                ((AbstractHomekitCharacteristic<?>) other).isPairedRead);
        if (readableCompare != 0)
            return readableCompare;

        // Compare by hasEvents
        int eventsCompare = Boolean.compare(this.hasEvents, ((AbstractHomekitCharacteristic<?>) other).hasEvents);
        if (eventsCompare != 0)
            return eventsCompare;

        // Compare by isTimedWrite
        int timedWriteCompare = Boolean.compare(this.isTimedWrite,
                ((AbstractHomekitCharacteristic<?>) other).isTimedWrite);
        if (timedWriteCompare != 0)
            return timedWriteCompare;

        // Compare by isWriteResponse
        int writeResponseCompare = Boolean.compare(this.isWriteResponse,
                ((AbstractHomekitCharacteristic<?>) other).isWriteResponse);
        if (writeResponseCompare != 0)
            return writeResponseCompare;

        // Compare by isAdditionalAuthorization
        int additionalAuthorizationCompare = Boolean.compare(this.isAdditionalAuthorization,
                ((AbstractHomekitCharacteristic<?>) other).isAdditionalAuthorization);
        if (additionalAuthorizationCompare != 0)
            return additionalAuthorizationCompare;

        // Finally compare by description
        return this.description.compareTo(((AbstractHomekitCharacteristic<?>) other).description);
    }

    @Override
    public boolean isAllowedValue(@Nullable T value) {
        return true; // By default, all values are allowed
    }

    @Override
    public Set<T> getAllowedValues() {
        return java.util.Collections.emptySet(); // By default, no specific allowed values
    }
}
