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
import java.util.Set;

import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * Abstract base class for HomeKit characteristics that handle long integer values.
 * This class extends {@link AbstractHomekitCharacteristic} to provide specialized handling for
 * long integer characteristics in the HomeKit protocol.
 *
 * <p>
 * The class implements long integer value management with:
 * <ul>
 * <li>Configurable value range (minValue to maxValue)</li>
 * <li>Configurable step size for value changes</li>
 * <li>Support for paired read/write operations</li>
 * <li>Event notification capabilities</li>
 * </ul>
 * </p>
 *
 * <p>
 * Key features:
 * <ul>
 * <li>Supports uint32 format as per HomeKit specification</li>
 * <li>Provides paired read/write access by default</li>
 * <li>Includes event notifications for value changes</li>
 * <li>Converts between JSON, OpenHAB states, and long values</li>
 * <li>Enforces value range and step size constraints</li>
 * </ul>
 * </p>
 *
 * <p>
 * The class integrates with:
 * <ul>
 * <li>{@link AbstractHomekitCharacteristic} - Base characteristic functionality</li>
 * <li>{@link HomekitService} - Service lifecycle management</li>
 * <li>{@link HomekitEventManager} - Event handling and notifications</li>
 * <li>{@link org.openhab.core.library.types.DecimalType} - State type for numeric values</li>
 * <li>{@link org.openhab.core.library.CoreItemFactory} - Item type factory for numbers</li>
 * </ul>
 * </p>
 *
 * @author Karel Goderis - Initial contribution
 * @version 1.0
 * @since 1.0
     */
public abstract class HomekitLongCharacteristic extends AbstractHomekitCharacteristic<Long> {

    private final long minValue;
    private final long maxValue;
    private final long minStep;

    /**
     * Creates a new long integer characteristic with specified value constraints.
     * This constructor initializes the characteristic with uint32 format and
     * enables paired read/write access and events.
     *
     * @param service the service this characteristic belongs to
     * @param eventManager the event manager for handling notifications
     * @param minValue the minimum allowed value (inclusive)
     * @param maxValue the maximum allowed value (inclusive)
     * @param minStep the minimum step size for value changes
     */
    public HomekitLongCharacteristic(HomekitService service, HomekitEventManager eventManager, long minValue,
            long maxValue, long minStep) {
        super(service, eventManager);
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.minStep = minStep;
        withFormat("uint32").withPairedWrite(true).withPairedRead(true).withEvents(true);
        initializeValue();
    }

    /**
     * Creates a new long integer characteristic from a JSON configuration.
     * This constructor parses the JSON value to initialize the characteristic,
     * including value constraints if specified.
     *
     * @param service the service this characteristic belongs to
     * @param eventManager the event manager for handling notifications
     * @param value the JSON configuration containing characteristic settings
     */
    public HomekitLongCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
        JsonObject jsonObject = (JsonObject) value;
        this.minValue = jsonObject.containsKey("minValue") ? jsonObject.getJsonNumber("minValue").longValue() : 0;
        this.maxValue = jsonObject.containsKey("maxValue") ? jsonObject.getJsonNumber("maxValue").longValue()
                : Long.MAX_VALUE;
        this.minStep = jsonObject.containsKey("minStep") ? jsonObject.getJsonNumber("minStep").longValue() : 1;
        initializeValue();
    }

    /**
     * Indicates that this characteristic is not hidden in the HomeKit interface.
     *
     * @return false, as long integer characteristics are always visible
     */
    @Override
    public boolean isHidden() {
        return false;
    }

    /**
     * Converts the characteristic to a JSON object with metadata.
     * Includes minimum value, maximum value, and step size.
     *
     * @return the JSON representation of the characteristic
     */
    @Override
    public JsonObject toJson() {
        JsonObject base = super.toJson();
        base = enrich(base, "minValue", minValue);
        base = enrich(base, "maxValue", maxValue);
        return enrich(base, "minStep", minStep);
    }

    /**
     * Converts the characteristic to a reduced JSON object.
     * Includes minimum value, maximum value, and step size.
     *
     * @return the reduced JSON representation of the characteristic
     */
    @Override
    public JsonObject toReducedJson() {
        JsonObject base = super.toReducedJson();
        base = enrich(base, "minValue", minValue);
        base = enrich(base, "maxValue", maxValue);
        return enrich(base, "minStep", minStep);
    }

    /**
     * Converts the characteristic to a JSON object with specified metadata.
     * Includes minimum value, maximum value, and step size.
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
        base = enrich(base, "minValue", minValue);
        base = enrich(base, "maxValue", maxValue);
        return enrich(base, "minStep", minStep);
    }

    /**
     * Converts a JSON value to a long integer.
     * Extracts the long value from a JsonNumber.
     *
     * @param value the JSON value to convert
     * @param conversionMap additional conversion parameters
     * @return the converted long value
     */
    @Override
    public Long toValue(JsonValue value, Map<String, Object> conversionMap) {
        return ((JsonNumber) value).longValue();
    }

    /**
     * Converts an OpenHAB state to a long integer.
     * Converts DecimalType states to their long equivalents.
     *
     * @param state the OpenHAB state to convert
     * @param conversionMap additional conversion parameters
     * @return the converted long value, minValue if conversion fails
     */
    @Override
    public Long toValue(State state, Map<String, Object> conversionMap) {
        DecimalType convertedState = state.as(DecimalType.class);
        if (convertedState == null) {
            return minValue;
        }
        return convertedState.longValue();
    }

    /**
     * Converts a long integer to an OpenHAB state.
     * Converts long values to DecimalType states.
     *
     * @param value the long value to convert
     * @return the converted DecimalType state
     */
    @Override
    public State toState(Long value) {
        return new DecimalType(value);
    }

    /**
     * Gets the default value for this characteristic.
     *
     * @return minValue as the default long value
     */
    @Override
    public Long getDefault() {
        return minValue;
    }

    /**
     * Gets the accepted OpenHAB item type for this characteristic.
     *
     * @return the item type string for numbers
     */
    public static String getAcceptedItemType() {
        return CoreItemFactory.NUMBER;
    }

    /**
     * Checks if a value is within the allowed range.
     *
     * @param value the value to check
     * @return true if the value is non-null and within range [minValue, maxValue]
     */
    @Override
    public boolean isAllowedValue(Long value) {
        return value != null && value >= minValue && value <= maxValue;
    }

    /**
     * Gets the set of all allowed values for this characteristic.
     * Since this characteristic supports a continuous range of values,
     * this method returns an empty set.
     *
     * @return an empty set, as values are constrained by range rather than enumeration
     */
    @Override
    public Set<Long> getAllowedValues() {
        return java.util.Collections.emptySet(); // No specific allowed values, just a range
    }
}
