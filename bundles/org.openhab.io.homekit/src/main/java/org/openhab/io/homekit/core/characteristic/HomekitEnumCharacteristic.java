package org.openhab.io.homekit.core.characteristic;

import java.util.Map;
import java.util.Set;

import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * Abstract base class for HomeKit characteristics that handle enumerated values.
 * This class extends {@link AbstractHomekitCharacteristic} to provide specialized handling for
 * enumerated characteristics in the HomeKit protocol.
 *
 * <p>
 * The class implements enumerated value management with:
 * <ul>
 *   <li>Integer-based value representation for enums</li>
 *   <li>Configurable value range and constraints</li>
 *   <li>Support for paired read/write operations</li>
 *   <li>Event notification capabilities</li>
 * </ul>
 * </p>
 *
 * <p>
 * Key features:
 * <ul>
 *   <li>Supports integer format as per HomeKit specification</li>
 *   <li>Provides paired read/write access by default</li>
 *   <li>Includes event notifications for value changes</li>
 *   <li>Converts between JSON, OpenHAB states, and integer values</li>
 *   <li>Enforces value range constraints (0 to maxValue)</li>
 * </ul>
 * </p>
 *
 * <p>
 * The class integrates with:
 * <ul>
 *   <li>{@link AbstractHomekitCharacteristic} - Base characteristic functionality</li>
 *   <li>{@link HomekitService} - Service lifecycle management</li>
 *   <li>{@link HomekitEventManager} - Event handling and notifications</li>
 *   <li>{@link org.openhab.core.library.types.DecimalType} - State type for numeric values</li>
 *   <li>{@link org.openhab.core.library.CoreItemFactory} - Item type factory for numbers</li>
 * </ul>
 * </p>
 *
 * @author Karel Goderis - Initial contribution
 * @version 1.0
 * @since 1.0
 */
@NonNullByDefault
public abstract class HomekitEnumCharacteristic extends AbstractHomekitCharacteristic<Integer> {

    private final int maxValue;

    /**
     * Creates a new enumerated characteristic with specified maximum value.
     * This constructor initializes the characteristic with integer format and
     * enables paired read/write access and events.
     *
     * @param service the service this characteristic belongs to
     * @param eventManager the event manager for handling notifications
     * @param maxValue the maximum allowed value (exclusive)
     */
    public HomekitEnumCharacteristic(HomekitService service, HomekitEventManager eventManager, int maxValue) {
        super(service, eventManager);
        this.maxValue = maxValue;
        withFormat("int").withPairedWrite(true).withPairedRead(true).withEvents(true);
        initializeValue();
    }

    /**
     * Creates a new enumerated characteristic from a JSON configuration.
     * This constructor parses the JSON value to initialize the characteristic,
     * including the maximum value if specified.
     *
     * @param service the service this characteristic belongs to
     * @param eventManager the event manager for handling notifications
     * @param value the JSON configuration containing characteristic settings
     */
    public HomekitEnumCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
        JsonObject jsonObject = (JsonObject) value;
        this.maxValue = jsonObject.containsKey("maxValue") ? jsonObject.getInt("maxValue") : 1;
        initializeValue();
    }

    /**
     * Indicates that this characteristic is not hidden in the HomeKit interface.
     *
     * @return false, as enumerated characteristics are always visible
     */
    @Override
    public boolean isHidden() {
        return false;
    }

    /**
     * Converts the characteristic to a JSON object with metadata.
     * Includes minimum value (0), maximum value, and step size (1).
     *
     * @return the JSON representation of the characteristic
     */
    @Override
    public JsonObject toJson() {
        JsonObject base = super.toJson();
        base = enrich(base, "minValue", 0);
        base = enrich(base, "maxValue", maxValue);
        return enrich(base, "minStep", 1);
    }

    /**
     * Converts the characteristic to a reduced JSON object.
     * Includes minimum value (0), maximum value, and step size (1).
     *
     * @return the reduced JSON representation of the characteristic
     */
    @Override
    public JsonObject toReducedJson() {
        JsonObject base = super.toReducedJson();
        base = enrich(base, "minValue", 0);
        base = enrich(base, "maxValue", maxValue);
        return enrich(base, "minStep", 1);
    }

    /**
     * Converts the characteristic to a JSON object with specified metadata.
     * Includes minimum value (0), maximum value, and step size (1).
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
        base = enrich(base, "minValue", 0);
        base = enrich(base, "maxValue", maxValue);
        return enrich(base, "minStep", 1);
    }

    /**
     * Converts a JSON value to an integer.
     * Handles numeric values and boolean values (true = 1, false = 0).
     *
     * @param value the JSON value to convert
     * @param conversionMap additional conversion parameters
     * @return the converted integer value
     * @throws IndexOutOfBoundsException if the value cannot be converted to an integer
     */
    @Override
    public Integer toValue(JsonValue value, Map<String, Object> conversionMap) {
        if (value instanceof JsonNumber jsonNumber) {
            return jsonNumber.intValue();
        } else if (value == JsonValue.TRUE) {
            return 1; // For at least one enum type (locks), homekit will send a true instead of 1
        } else if (value == JsonValue.FALSE) {
            return 0;
        } else {
            throw new IndexOutOfBoundsException(
                    "Cannot convert " + (value != null ? value.getClass() : "null") + " to Integer");
        }
    }

    /**
     * Converts an OpenHAB state to an integer.
     * Converts DecimalType states to their integer equivalents.
     *
     * @param state the OpenHAB state to convert
     * @param conversionMap additional conversion parameters
     * @return the converted integer value, 0 if conversion fails
     */
    @Override
    public Integer toValue(State state, Map<String, Object> conversionMap) {
        DecimalType convertedState = state.as(DecimalType.class);
        if (convertedState == null) {
            return 0;
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
     * @return 0 as the default integer value
     */
    @Override
    public Integer getDefault() {
        return 0;
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
     * @return true if the value is non-null and within range [0, maxValue)
     */
    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= 0 && value < maxValue;
    }

    /**
     * Gets the set of all allowed values for this characteristic.
     *
     * @return a set containing all integers from 0 to maxValue-1
     */
    @Override
    public Set<Integer> getAllowedValues() {
        Set<Integer> values = new java.util.HashSet<>();
        for (int i = 0; i < maxValue; i++) {
            values.add(i);
        }
        return values;
    }
}
