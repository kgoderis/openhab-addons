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
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/*
 * Abstract base class for HomeKit characteristics that handle integer values.
 * This class extends {@link AbstractHomekitCharacteristic} to provide specialized handling for
 * integer characteristics in the HomeKit protocol.
 *
 * <p>
 * The class implements integer value management with:
 * <ul>
 * <li>Configurable value range (minValue to maxValue)</li>
 * <li>Unit specification for value representation</li>
 * <li>Support for paired read/write operations</li>
 * <li>Event notification capabilities</li>
 * </ul>
 * </p>
 *
 * <p>
 * Key features:
 * <ul>
 * <li>Supports integer format as per HomeKit specification</li>
 * <li>Provides paired read/write access by default</li>
 * <li>Includes event notifications for value changes</li>
 * <li>Converts between JSON, OpenHAB states, and integer values</li>
 * <li>Enforces value range constraints</li>
 * <li>Supports unit specification for value representation</li>
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
@NonNullByDefault
public abstract class HomekitIntegerCharacteristic extends AbstractHomekitCharacteristic<Integer> {

    protected final int minValue;
    private final int maxValue;
    private final String unit;

    /**
     * Creates a new integer characteristic with specified value constraints and unit.
     * This constructor initializes the characteristic with integer format and
     * enables paired read/write access and events.
     *
     * @param service the service this characteristic belongs to
     * @param eventManager the event manager for handling notifications
     * @param minValue the minimum allowed value (inclusive)
     * @param maxValue the maximum allowed value (inclusive)
     * @param unit the unit of measurement for the values
     */
    public HomekitIntegerCharacteristic(HomekitService service, HomekitEventManager eventManager, int minValue,
            int maxValue, String unit) {
        super(service, eventManager);
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.unit = unit;
        withFormat("int").withPairedWrite(true).withPairedRead(true).withEvents(true);
        initializeValue();
    }

    /**
     * Creates a new integer characteristic from a JSON configuration.
     * This constructor parses the JSON value to initialize the characteristic,
     * including value constraints and unit if specified.
     *
     * @param service the service this characteristic belongs to
     * @param eventManager the event manager for handling notifications
     * @param value the JSON configuration containing characteristic settings
     */
    public HomekitIntegerCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
        JsonObject jsonObject = (JsonObject) value;
        this.minValue = jsonObject.containsKey("minValue") ? jsonObject.getInt("minValue") : 0;
        this.maxValue = jsonObject.containsKey("maxValue") ? jsonObject.getInt("maxValue") : Integer.MAX_VALUE;
        this.unit = jsonObject.containsKey("unit") ? jsonObject.getString("unit") : "";
        initializeValue();
    }

    /**
     * Indicates that this characteristic is not hidden in the HomeKit interface.
     *
     * @return false, as integer characteristics are always visible
     */
    @Override
    public boolean isHidden() {
        return false;
    }

    /**
     * Converts the characteristic to a JSON object with metadata.
     * Includes minimum value, maximum value, step size, and unit.
     *
     * @return the JSON representation of the characteristic
     */
    @Override
    public JsonObject toJson() {
        JsonObject base = super.toJson();
        base = enrich(base, "minValue", minValue);
        base = enrich(base, "maxValue", maxValue);
        base = enrich(base, "minStep", 1);
        return enrich(base, "unit", unit);
    }

    /**
     * Converts the characteristic to a reduced JSON object.
     * Includes minimum value, maximum value, step size, and unit.
     *
     * @return the reduced JSON representation of the characteristic
     */
    @Override
    public JsonObject toReducedJson() {
        JsonObject base = super.toReducedJson();
        base = enrich(base, "minValue", minValue);
        base = enrich(base, "maxValue", maxValue);
        base = enrich(base, "minStep", 1);
        return enrich(base, "unit", unit);
    }

    /**
     * Converts the characteristic to a JSON object with specified metadata.
     * Includes minimum value, maximum value, step size, and unit.
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
        base = enrich(base, "minStep", 1);
        return enrich(base, "unit", unit);
    }

    /**
     * Converts a JSON value to an integer.
     * Extracts the integer value from a JsonNumber.
     *
     * @param value the JSON value to convert
     * @param conversionMap additional conversion parameters
     * @return the converted integer value
     */
    @Override
    public Integer toValue(JsonValue value, Map<String, Object> conversionMap) {
        return ((JsonNumber) value).intValue();
    }

    /**
     * Converts an OpenHAB state to an integer.
     * Converts DecimalType states to their integer equivalents.
     *
     * @param state the OpenHAB state to convert
     * @param conversionMap additional conversion parameters
     * @return the converted integer value, minValue if conversion fails
     */
    @Override
    public Integer toValue(State state, Map<String, Object> conversionMap) {
        DecimalType convertedState = state.as(DecimalType.class);
        if (convertedState == null) {
            return minValue;
        }
        return convertedState.intValue();
    }

    /**
     * Converts an integer to an OpenHAB state.
     * Converts integer values to DecimalType states.
     *
     * @param value the integer value to convert
     * @return the converted DecimalType state
     */
    @Override
    public State toState(Integer value) {
        return new DecimalType(value);
    }

    /**
     * Gets the default value for this characteristic.
     *
     * @return minValue as the default integer value
     */
    @Override
    public Integer getDefault() {
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
}
