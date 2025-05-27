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
 * Abstract base class for HomeKit characteristics that handle floating-point values.
 * This class extends {@link AbstractHomekitCharacteristic} to provide specialized handling for
 * floating-point characteristics in the HomeKit protocol.
 *
 * <p>
 * The class implements floating-point value management with:
 * <ul>
 *   <li>Configurable value range (minValue to maxValue)</li>
 *   <li>Configurable step size for value changes</li>
 *   <li>Unit specification for value representation</li>
 *   <li>Support for paired read/write operations</li>
 *   <li>Event notification capabilities</li>
 * </ul>
 * </p>
 *
 * <p>
 * Key features:
 * <ul>
 *   <li>Supports float format as per HomeKit specification</li>
 *   <li>Provides paired read/write access by default</li>
 *   <li>Includes event notifications for value changes</li>
 *   <li>Converts between JSON, OpenHAB states, and double values</li>
 *   <li>Enforces value range and step size constraints</li>
 *   <li>Supports unit specification for value representation</li>
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
public abstract class HomekitFloatCharacteristic extends AbstractHomekitCharacteristic<Double> {

    protected final double minValue;
    private final double maxValue;
    private final double minStep;
    private final String unit;

    /**
     * Creates a new floating-point characteristic with specified value constraints and unit.
     * This constructor initializes the characteristic with float format and
     * enables paired read/write access and events.
     *
     * @param service the service this characteristic belongs to
     * @param eventManager the event manager for handling notifications
     * @param minValue the minimum allowed value (inclusive)
     * @param maxValue the maximum allowed value (inclusive)
     * @param minStep the minimum step size for value changes
     * @param unit the unit of measurement for the values
     */
    public HomekitFloatCharacteristic(HomekitService service, HomekitEventManager eventManager, double minValue,
            double maxValue, double minStep, String unit) {
        super(service, eventManager);
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.minStep = minStep;
        this.unit = unit;
        withFormat("float").withPairedWrite(true).withPairedRead(true).withEvents(true);
        initializeValue();
    }

    /**
     * Creates a new floating-point characteristic from a JSON configuration.
     * This constructor parses the JSON value to initialize the characteristic,
     * including value constraints, step size, and unit if specified.
     *
     * @param service the service this characteristic belongs to
     * @param eventManager the event manager for handling notifications
     * @param value the JSON configuration containing characteristic settings
     */
    public HomekitFloatCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
        JsonObject jsonObject = (JsonObject) value;
        this.minValue = jsonObject.containsKey("minValue") ? jsonObject.getJsonNumber("minValue").doubleValue() : 0;
        this.maxValue = jsonObject.containsKey("maxValue") ? jsonObject.getJsonNumber("maxValue").doubleValue() : 100;
        this.minStep = jsonObject.containsKey("minStep") ? jsonObject.getJsonNumber("minStep").doubleValue() : 1;
        this.unit = jsonObject.containsKey("unit") ? jsonObject.getString("unit") : "";
        initializeValue();
    }

    /**
     * Indicates that this characteristic is not hidden in the HomeKit interface.
     *
     * @return false, as floating-point characteristics are always visible
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
        base = enrich(base, "minStep", minStep);
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
        base = enrich(base, "minStep", minStep);
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
        base = enrich(base, "minStep", minStep);
        return enrich(base, "unit", unit);
    }

    /**
     * Converts a JSON value to a double.
     * Extracts the double value from a JsonNumber.
     *
     * @param value the JSON value to convert
     * @param conversionMap additional conversion parameters
     * @return the converted double value
     */
    @Override
    public Double toValue(JsonValue value, Map<String, Object> conversionMap) {
        return ((JsonNumber) value).doubleValue();
    }

    /**
     * Converts an OpenHAB state to a double.
     * Converts DecimalType states to their double equivalents.
     *
     * @param state the OpenHAB state to convert
     * @param conversionMap additional conversion parameters
     * @return the converted double value, minValue if conversion fails
     */
    @Override
    public Double toValue(State state, Map<String, Object> conversionMap) {
        DecimalType convertedState = state.as(DecimalType.class);
        if (convertedState == null) {
            return minValue;
        }
        return convertedState.doubleValue();
    }

    /**
     * Converts a double to an OpenHAB state.
     * Converts double values to DecimalType states.
     *
     * @param value the double value to convert
     * @return the converted DecimalType state
     */
    @Override
    public State toState(Double value) {
        return new DecimalType(value);
    }

    /**
     * Gets the default value for this characteristic.
     *
     * @return minValue as the default double value
     */
    @Override
    public Double getDefault() {
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
    public boolean isAllowedValue(Double value) {
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
    public Set<Double> getAllowedValues() {
        return java.util.Collections.emptySet(); // No specific allowed values, just a range
    }
}
