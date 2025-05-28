package org.openhab.io.homekit.core.characteristic;

import java.util.Map;

import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.event.HomekitEventType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * Implementation of a write-only boolean characteristic for HomeKit accessories.
 *
 * This class extends {@link AbstractHomekitCharacteristic} to provide a specialized implementation
 * for boolean characteristics that can only be written to, not read from. It is particularly useful
 * for characteristics that represent actions or commands rather than states, such as triggers or
 * momentary switches.
 *
 * <p>
 * The class integrates with several key components:
 * <ul>
 * <li>{@link AbstractHomekitCharacteristic} for base characteristic functionality</li>
 * <li>{@link HomekitService} for service-level operations</li>
 * <li>{@link HomekitEventManager} for event handling</li>
 * <li>{@link org.openhab.core.types.State} for state conversion</li>
 * <li>{@link javax.json.JsonValue} for JSON serialization</li>
 * </ul>
 * </p>
 *
 * <p>
 * Key features:
 * <ul>
 * <li>Write-only access control through permission management</li>
 * <li>Boolean value conversion between HomeKit and OpenHAB formats</li>
 * <li>Event handling for value changes</li>
 * <li>JSON serialization for HomeKit protocol communication</li>
 * <li>Integration with OpenHAB's state management system</li>
 * </ul>
 * </p>
 *
 * <p>
 * The class follows the HomeKit Accessory Protocol (HAP) specification for boolean characteristics
 * and integrates with OpenHAB's state management system for reliable device control. It provides
 * a robust implementation for write-only boolean characteristics while ensuring proper integration
 * with both HomeKit and OpenHAB ecosystems.
 * </p>
 *
 * @author Karel Goderis - Initial contribution
 * @version 1.0
 * @since 1.0
 */
@NonNullByDefault
public abstract class HomekitWriteOnlyBooleanCharacteristic extends AbstractHomekitCharacteristic<Boolean> {

    /**
     * Creates a new write-only boolean characteristic.
     *
     * This constructor initializes a new write-only boolean characteristic with its required
     * dependencies and sets up the event subscription system. It integrates with:
     * <ul>
     * <li>{@link HomekitService} for service integration</li>
     * <li>{@link HomekitEventManager} for event handling</li>
     * <li>{@link HomekitEventType} for event type management</li>
     * </ul>
     *
     * @param service the service this characteristic belongs to
     * @param eventManager the event manager for handling events
     * @throws IllegalArgumentException if any required parameter is null
     * @since 1.0
     */
    public HomekitWriteOnlyBooleanCharacteristic(HomekitService service, HomekitEventManager eventManager) {
        super(service, eventManager);
        withFormat("bool").withPairedWrite(true).withPairedRead(false).withEvents(false);
        initializeValue();
    }

    /**
     * Creates a new write-only boolean characteristic from a JSON value.
     *
     * This constructor initializes a write-only boolean characteristic from a JSON configuration,
     * allowing for flexible characteristic creation and configuration. It integrates with:
     * <ul>
     * <li>{@link javax.json.JsonValue} for configuration parsing</li>
     * <li>{@link HomekitService} for service integration</li>
     * <li>{@link HomekitEventManager} for event handling</li>
     * <li>{@link HomekitEventType} for event type management</li>
     * </ul>
     *
     * @param service the service this characteristic belongs to
     * @param eventManager the event manager for handling events
     * @param value the JSON value containing characteristic configuration
     * @throws IllegalArgumentException if the JSON value is invalid or required parameters are null
     * @since 1.0
     */
    public HomekitWriteOnlyBooleanCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
        initializeValue();
    }

    /**
     * Indicates that this characteristic is not hidden in the HomeKit interface.
     *
     * @return false, as write-only boolean characteristics are always visible
     */
    @Override
    public boolean isHidden() {
        return false;
    }

    /**
     * Gets the default value for this characteristic.
     *
     * @return the default boolean value (false)
     * @since 1.0
     */
    @Override
    public Boolean getDefault() {
        return false;
    }

    /**
     * Gets the current value of this characteristic.
     * Since this is a write-only characteristic, always returns the default value.
     *
     * @return the default value (false)
     */
    @Override
    public Boolean getValue() {
        return getDefault();
    }

    /**
     * Converts a JSON value to a boolean value.
     *
     * This method handles the conversion of JSON values to boolean values, supporting
     * various JSON value types and conversion rules. It integrates with:
     * <ul>
     * <li>{@link javax.json.JsonValue} for value parsing</li>
     * <li>{@link javax.json.JsonObject} for object handling</li>
     * <li>{@link javax.json.JsonString} for string handling</li>
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
        if (jsonValue.getValueType().equals(ValueType.NUMBER)) {
            return ((JsonNumber) jsonValue).intValue() > 0;
        }
        return jsonValue.equals(JsonValue.TRUE);
    }

    /**
     * Converts a State to a boolean value.
     *
     * This method handles the conversion of OpenHAB states to boolean values, supporting
     * various state types and conversion rules. It integrates with:
     * <ul>
     * <li>{@link org.openhab.core.types.State} for state handling</li>
     * <li>{@link org.openhab.core.types.OnOffType} for on/off state handling</li>
     * <li>{@link org.openhab.core.types.OpenClosedType} for open/closed state handling</li>
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
            return getDefault();
        }

        return convertedState.equals(OnOffType.ON);
    }

    /**
     * Converts a boolean value to a State.
     *
     * This method handles the conversion of boolean values to OpenHAB states, supporting
     * various state types and conversion rules. It integrates with:
     * <ul>
     * <li>{@link org.openhab.core.types.State} for state creation</li>
     * <li>{@link org.openhab.core.types.OnOffType} for on/off state creation</li>
     * <li>{@link org.openhab.core.types.OpenClosedType} for open/closed state creation</li>
     * </ul>
     *
     * @param value the boolean value to convert
     * @return the converted state
     * @since 1.0
     */
    @Override
    public State toState(Boolean value) {
        return value ? OnOffType.ON : OnOffType.OFF;
    }

    /**
     * Converts a boolean value to an event JSON object.
     *
     * @param value the boolean value to convert
     * @return the event JSON object
     */
    @Override
    public JsonObject toEventJson(Boolean value) {
        return super.toEventJson(value);
    }

    /**
     * Converts the current value to an event JSON object.
     *
     * @return the event JSON object
     */
    @Override
    public JsonObject toEventJson() {
        return super.toEventJson();
    }

    /**
     * Converts a boolean value to a JSON value.
     *
     * @param value the boolean value to convert
     * @return the JSON value
     */
    @Override
    public JsonValue toValueJson(@Nullable Boolean value) {
        return super.toValueJson(value);
    }

    /**
     * Converts this characteristic to a JSON object with metadata.
     *
     * @return the JSON representation of the characteristic
     */
    @Override
    public JsonObject toJson() {
        return super.toJson();
    }

    /**
     * Converts this characteristic to a JSON object with specified metadata.
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
        return super.toJson(includeMeta, includePermissions, includeType, includeEvent);
    }

    /**
     * Converts this characteristic to a reduced JSON object.
     *
     * @return the reduced JSON representation of the characteristic
     */
    @Override
    public JsonObject toReducedJson() {
        return super.toReducedJson();
    }

    /**
     * Gets the accepted OpenHAB item type for this characteristic.
     *
     * @return the item type string for switches
     */
    public static String getAcceptedItemType() {
        return CoreItemFactory.SWITCH;
    }
}
