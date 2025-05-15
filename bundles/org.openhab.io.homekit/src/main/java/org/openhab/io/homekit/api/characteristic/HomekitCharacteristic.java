package org.openhab.io.homekit.api.characteristic;

import java.util.Set;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.Identifiable;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitCharacteristicUID;

//TODO https://github.com/jlusiardi/homekit_python/blob/master/homekit/model/characteristics/characteristic_types.py
//TODO https://github.com/apple/HomekitADK/blob/master/HAP/HAPCharacteristicTypes.h
//TODO https://github.com/apple/HomekitADK/blob/master/HAP/HAPCharacteristicTypes.h

/**
 * Interface for the Characteristics provided by a HomekitService.
 *
 * <p>
 * Characteristics are the lowest level building block of the Homekit HomekitAccessory Protocol. They
 * define variables that can be retrieved or set by the remote client.
 *
 * @author Andy Lintner
 */
@NonNullByDefault
public interface HomekitCharacteristic<T>
        extends Identifiable<HomekitCharacteristicUID>, Comparable<HomekitCharacteristic<?>> {

    // Core identification and type methods
    /**
     * Gets the unique identifier for this characteristic.
     *
     * @return the HomekitCharacteristicUID that uniquely identifies this characteristic
     */
    @Override
    HomekitCharacteristicUID getUID();

    /**
     * Gets the instance ID of this characteristic.
     * HomekitCharacteristic Instance IDs are assigned from the same number pool that is unique within each
     * HomekitAccessory. For example, if the first HomekitCharacteristic has an Instance Id of "1", then
     * no other HomekitCharacteristic can have an Instance Id of "1" within the parent HomekitAccessory object.
     * After a firmware update, HomekitCharacteristic types that remain unchanged must retain their previous instance
     * Ids, newly added HomekitCharacteristic must not reuse Instance Ids from Characteristics that were removed
     * in the firmware update.
     *
     * @return the unique instance identifier
     */
    long getInstanceId();

    /**
     * Gets the parent service that contains this characteristic.
     *
     * @return the parent HomekitService
     */
    HomekitService getService();

    /**
     * Gets the type identifier of this characteristic.
     *
     * @return the type identifier string
     */
    String getType();

    /**
     * Gets the tag associated with this characteristic.
     *
     * @return the tag string
     */
    String getTag();

    /**
     * Gets the human-readable description of this characteristic.
     *
     * @return the description string
     */
    String getDescription();

    /**
     * Checks if this characteristic is of the specified type.
     *
     * @param aType the type to check against
     * @return true if the characteristic is of the specified type
     */
    boolean isType(String aType);

    /**
     * Checks if this characteristic is hidden from user interfaces.
     *
     * @return true if the characteristic is hidden
     */
    boolean isHidden();

    /**
     * Checks if this characteristic is mandatory.
     *
     * @return true if the characteristic is mandatory
     */
    boolean isMandatory();

    // Value and state management methods
    /**
     * Gets the current value of the characteristic.
     *
     * @return the current value of type T
     */
    T getValue();

    /**
     * Sets the value of the characteristic.
     *
     * @param value the new value to set
     * @throws Exception if the value cannot be set
     */
    void setValue(T value) throws Exception;

    /**
     * Sets the value of the characteristic from a JSON value.
     *
     * @param jsonValue the JSON value to convert and set
     * @throws Exception if the value cannot be set
     */
    void setValue(JsonValue jsonValue) throws Exception;

    /**
     * Gets the default value for this characteristic.
     *
     * @return the default value of type T
     */
    T getDefault();

    // State conversion methods
    /**
     * Converts a JSON value to the characteristic's type.
     *
     * @param jsonValue the JSON value to convert
     * @return the converted value of type T
     */
    T toValue(JsonValue jsonValue);

    /**
     * Converts a State to the characteristic's type.
     *
     * @param state the state to convert
     * @return the converted value of type T
     */
    T toValue(State state);

    /**
     * Converts the characteristic's type to a State.
     *
     * @param value the value to convert
     * @return the converted state
     */
    State toState(T value);

    /**
     * Converts a JSON value to a State.
     *
     * @param jsonValue the JSON value to convert
     * @return the converted state
     */
    State toState(JsonValue jsonValue);

    // JSON conversion methods
    /**
     * Creates the JSON representation of the characteristic, in accordance with the Homekit HomekitAccessory Protocol.
     *
     * @return the resulting JSON object
     */
    JsonObject toJson();

    /**
     * Creates a reduced JSON representation of the characteristic, excluding some optional fields.
     *
     * @return the resulting JSON object
     */
    JsonObject toReducedJson();

    /**
     * Creates a JSON representation of the characteristic with specified fields included.
     *
     * @param includeMeta whether to include metadata (iid, aid, type)
     * @param includePermissions whether to include permissions
     * @param includeType whether to include type information
     * @param includeEvent whether to include event information
     * @return the resulting JSON object
     */
    JsonObject toJson(boolean includeMeta, boolean includePermissions, boolean includeType, boolean includeEvent);

    /**
     * Creates the JSON representation of an event for the characteristic.
     *
     * @return the resulting JSON object
     */
    JsonObject toEventJson();

    /**
     * Creates the JSON representation of an event for the characteristic with a specific value.
     *
     * @param value the value to include in the event
     * @return the resulting JSON object
     */
    JsonObject toEventJson(T value);

    /**
     * Converts a value to its JSON representation.
     *
     * @param value the value to convert
     * @return the JSON representation
     */
    JsonValue toValueJson(T value);

    /**
     * Converts a state to its JSON representation.
     *
     * @param state the state to convert
     * @return the JSON representation
     */
    JsonValue toValueJson(State state);

    // Builder pattern methods
    /**
     * Sets the instance ID for this characteristic.
     *
     * @param instanceId the instance ID to set
     * @return this instance for method chaining
     */
    HomekitCharacteristic<T> withInstanceId(long instanceId);

    /**
     * Sets whether this characteristic is hidden. Marks the characteristic as hidden from the user
     * interface. This is used for internal or advanced features not meant for regular user
     * interaction.
     *
     * @param isHidden whether the characteristic is hidden
     * @return this instance for method chaining
     */
    HomekitCharacteristic<T> withHidden(boolean isHidden);

    /**
     * Sets whether this characteristic is mandatory.
     *
     * @param isMandatory whether the characteristic is mandatory
     * @return this instance for method chaining
     */
    HomekitCharacteristic<T> withMandatory(boolean isMandatory);

    /**
     * Sets whether this characteristic has events. Enables the accessory to send notifications to
     * paired controllers when the characteristic's value changes. This is used for real-time
     * updates, such as a sensor reporting a new reading.
     *
     * @param hasEvents whether the characteristic has events
     * @return this instance for method chaining
     */
    HomekitCharacteristic<T> withEvents(boolean hasEvents);

    /**
     * Sets the format for this characteristic.
     *
     * @param format the format to set
     * @return this instance for method chaining
     */
    HomekitCharacteristic<T> withFormat(String format);

    /**
     * Sets the description for this characteristic.
     *
     * @param description the description to set
     * @return this instance for method chaining
     */
    HomekitCharacteristic<T> withDescription(String description);

    /**
     * Sets whether this characteristic is writable. Allows a paired controller to change the value
     * of the characteristic. This permission is required for any characteristic that users or
     * automations should be able to control or modify.
     *
     * @param isWritable whether the characteristic is writable
     * @return this instance for method chaining
     */
    HomekitCharacteristic<T> withPairedWrite(boolean isWritable);

    /**
     * Sets whether this characteristic is readable. Allows a paired HomeKit controller (like an
     * iPhone or HomePod) to read the value of the characteristic. This is the standard permission
     * for characteristics whose value should be visible to users.
     *
     * @param isReadable whether the characteristic is readable
     * @return this instance for method chaining
     */
    HomekitCharacteristic<T> withPairedRead(boolean isReadable);

    /**
     * Sets whether this characteristic is timed write. Timed Write is a permission in the HomeKit
     * Accessory Protocol (HAP) that indicates a characteristic requires a special write procedure
     * involving a time window. When a characteristic is marked with the "timed write" permission,
     * a controller (such as an iPhone or HomePod) must first request a timed write session from
     * the accessory. The accessory then grants a limited time window during which the controller
     * can perform the write operation.
     * 
     * If a standard write request is sent to a characteristic that requires timed write, the
     * accessory must respond with an error, indicating that the timed write procedure must be used
     * instead. This mechanism is designed for characteristics where it is important to tightly
     * control the timing of value changes, such as for security-sensitive operations or actions
     * that must be completed within a specific timeframe.
     *
     * @param isTimedWrite whether the characteristic is timed write
     * @return this instance for method chaining
     */
    HomekitCharacteristic<T> withTimedWrite(boolean isTimedWritable);

    /**
     * Sets whether this characteristic requires a write response. Indicates that the accessory must
     * send a response after a write operation is performed, confirming the result of the write.
     *
     * @param isWriteResponse whether the characteristic requires a write response
     * @return this instance for method chaining
     */
    HomekitCharacteristic<T> withWriteResponse(boolean isWriteResponse);

    /**
     * Sets whether this characteristic requires additional authorization. Indicates that extra
     * authorization is required to write to this characteristic. This might be used for sensitive
     * operations that require confirmation or authentication beyond standard permissions.
     *
     * @param isAdditionalAuthorization whether the characteristic requires additional authorization
     * @return this instance for method chaining
     */
    HomekitCharacteristic<T> withAdditionalAuthorization(boolean isAdditionalAuthorization);

    // Utility methods

    /**
     * Updates this characteristic with values from another characteristic.
     *
     * @param other the characteristic to copy values from
     */
    void updateWith(HomekitCharacteristic<?> other);

    /**
     * Checks if a value is allowed for this characteristic.
     *
     * @param value the value to check
     * @return true if the value is allowed
     */
    boolean isAllowedValue(T value);

    /**
     * Gets all allowed values for this characteristic.
     *
     * @return an array of allowed values
     */
    Set<T> getAllowedValues();
}
