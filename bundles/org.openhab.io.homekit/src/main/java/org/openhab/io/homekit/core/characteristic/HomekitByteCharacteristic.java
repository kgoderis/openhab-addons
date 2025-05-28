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

/**
 * Abstract base class for HomeKit characteristics that handle byte values within a specified range.
 * This class extends {@link AbstractHomekitCharacteristic} to provide specialized handling for byte-based
 * characteristics in the HomeKit protocol.
 *
 * <p>
 * The class implements value management for byte characteristics with:
 * <ul>
 * <li>Configurable minimum and maximum value constraints</li>
 * <li>Type conversion between HomeKit byte values and OpenHAB states</li>
 * <li>JSON serialization with range information</li>
 * <li>Default value handling based on minimum value</li>
 * </ul>
 * </p>
 *
 * <p>
 * Key features:
 * <ul>
 * <li>Supports uint8 format as per HomeKit specification</li>
 * <li>Provides paired read/write access by default</li>
 * <li>Includes event notifications for value changes</li>
 * <li>Maintains value constraints through min/max boundaries</li>
 * <li>Integrates with OpenHAB's number item type system</li>
 * </ul>
 * </p>
 *
 * <p>
 * The class integrates with:
 * <ul>
 * <li>{@link AbstractHomekitCharacteristic} - Base characteristic functionality</li>
 * <li>{@link HomekitService} - Service lifecycle management</li>
 * <li>{@link HomekitEventManager} - Event handling and notifications</li>
 * <li>{@link org.openhab.core.library.types.DecimalType} - State conversion</li>
 * <li>{@link org.openhab.core.library.CoreItemFactory} - Item type validation</li>
 * </ul>
 * </p>
 *
 * @author Karel Goderis - Initial contribution
 * @version 1.0
 * @since 1.0
 */
@NonNullByDefault
public abstract class HomekitByteCharacteristic extends AbstractHomekitCharacteristic<Byte> {

    private final byte minValue;
    private final byte maxValue;

    /**
     * Creates a new byte characteristic with specified value range constraints.
     * This constructor initializes the characteristic with the given minimum and maximum values,
     * setting up the appropriate format and permissions.
     *
     * @param service the service this characteristic belongs to
     * @param eventManager the event manager for handling notifications
     * @param minValue the minimum allowed value for this characteristic
     * @param maxValue the maximum allowed value for this characteristic
     */
    public HomekitByteCharacteristic(HomekitService service, HomekitEventManager eventManager, byte minValue,
            byte maxValue) {
        super(service, eventManager);
        this.minValue = minValue;
        this.maxValue = maxValue;
        withFormat("uint8").withPairedWrite(true).withPairedRead(true).withEvents(true);
        initializeValue();
    }

    /**
     * Creates a new byte characteristic from a JSON configuration.
     * This constructor parses the JSON value to extract range constraints and
     * initializes the characteristic accordingly.
     *
     * @param service the service this characteristic belongs to
     * @param eventManager the event manager for handling notifications
     * @param value the JSON configuration containing characteristic settings
     */
    public HomekitByteCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
        JsonObject jsonObject = (JsonObject) value;
        this.minValue = jsonObject.containsKey("minValue") ? (byte) jsonObject.getInt("minValue") : 0;
        this.maxValue = jsonObject.containsKey("maxValue") ? (byte) jsonObject.getInt("maxValue") : Byte.MAX_VALUE;
        initializeValue();
    }

    /**
     * Indicates that this characteristic is not hidden in the HomeKit interface.
     *
     * @return false, as byte characteristics are always visible
     */
    @Override
    public boolean isHidden() {
        return false;
    }

    /**
     * Converts this characteristic to a JSON representation including range information.
     * The JSON includes minimum value, maximum value, and minimum step size.
     *
     * @return JSON object representing this characteristic
     */
    @Override
    public JsonObject toJson() {
        JsonObject base = super.toJson();
        base = enrich(base, "minValue", minValue);
        base = enrich(base, "maxValue", maxValue);
        return enrich(base, "minStep", 1);
    }

    /**
     * Converts this characteristic to a reduced JSON representation.
     * Similar to {@link #toJson()} but with minimal required information.
     *
     * @return reduced JSON object representing this characteristic
     */
    @Override
    public JsonObject toReducedJson() {
        JsonObject base = super.toReducedJson();
        base = enrich(base, "minValue", minValue);
        base = enrich(base, "maxValue", maxValue);
        return enrich(base, "minStep", 1);
    }

    /**
     * Converts this characteristic to a JSON representation with configurable detail level.
     *
     * @param includeMeta whether to include metadata
     * @param includePermissions whether to include permissions
     * @param includeType whether to include type information
     * @param includeEvent whether to include event information
     * @return JSON object with specified detail level
     */
    @Override
    public JsonObject toJson(boolean includeMeta, boolean includePermissions, boolean includeType,
            boolean includeEvent) {
        JsonObject base = super.toJson(includeMeta, includePermissions, includeType, includeEvent);
        base = enrich(base, "minValue", minValue);
        base = enrich(base, "maxValue", maxValue);
        return enrich(base, "minStep", 1);
    }

    /**
     * Converts a JSON value to a byte value.
     *
     * @param value the JSON value to convert
     * @param conversionMap additional conversion parameters
     * @return the converted byte value
     */
    @Override
    public Byte toValue(JsonValue value, Map<String, Object> conversionMap) {
        return (byte) ((JsonNumber) value).intValue();
    }

    /**
     * Converts an OpenHAB state to a byte value.
     * If the state cannot be converted to a DecimalType, returns the minimum value.
     *
     * @param state the OpenHAB state to convert
     * @param conversionMap additional conversion parameters
     * @return the converted byte value
     */
    @Override
    public Byte toValue(State state, Map<String, Object> conversionMap) {
        DecimalType convertedState = state.as(DecimalType.class);
        if (convertedState == null) {
            return minValue;
        }
        return (byte) convertedState.intValue();
    }

    /**
     * Converts a byte value to an OpenHAB state.
     *
     * @param value the byte value to convert
     * @return the converted DecimalType state
     */
    @Override
    public State toState(Byte value) {
        return new DecimalType(value);
    }

    /**
     * Returns the default value for this characteristic.
     * The default value is the minimum allowed value.
     *
     * @return the minimum value as the default
     */
    @Override
    public Byte getDefault() {
        return minValue;
    }

    /**
     * Returns the accepted OpenHAB item type for this characteristic.
     *
     * @return the number item type identifier
     */
    public static String getAcceptedItemType() {
        return CoreItemFactory.NUMBER;
    }
}
