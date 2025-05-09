package org.openhab.io.homekit.api.characteristic;

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

    /**
     * HomekitCharacteristic Instance IDs are assigned from the same number pool that is unique within each
     * HomekitAccessory. For example, if the first HomekitCharacteristic has an Instance Id of "1", then
     * no other HomekitCharacteristic can have an Instance Id of "1" within the parent HomekitAccessory object.
     * After a firmware update, HomekitCharacteristic types that remain unchanged must retain their previous instance
     * Ids, newly added HomekitCharacteristic must not reuse Instance Ids from Characteristics that were removed
     * in the firmware update.
     *
     * @return the unique identifier.
     */
    long getInstanceId();

    /**
     * Gets the parent HomekitService that contains this HomekitCharacteristic.
     *
     * @return the parent HomekitService
     */
    HomekitService getService();

    /**
     * Checks if this HomekitCharacteristic is of the specified type.
     *
     * @param aType the type to check against
     * @return true if the HomekitCharacteristic is of the specified type
     */
    boolean isType(String aType);

    // Static method can't be called through instance
    static String getType() {
        throw new UnsupportedOperationException("Must be implemented by characteristic class");
    }

    /**
     * Gets the instance type of this HomekitCharacteristic.
     *
     * @return the instance type
     */
    String getInstanceType();

    /**
     * Services may specify the Characteristics that are to be hidden
     *
     * @return true if the HomekitCharacteristic is hidden
     */
    boolean isHidden();

    /**
     * Sets whether events are enabled for this HomekitCharacteristic.
     *
     * @param value true to enable events, false to disable
     */
    void setHasEvents(boolean value);

    /**
     * Creates a JSON representation of the HomekitCharacteristic with specified fields included.
     *
     * @param includeMeta whether to include metadata (iid, aid, type)
     * @param includePermissions whether to include permissions
     * @param includeType whether to include type information
     * @param includeEvent whether to include event information
     * @return the resulting JSON object
     */
    JsonObject toJson(boolean includeMeta, boolean includePermissions, boolean includeType, boolean includeEvent);

    /**
     * Creates the JSON representation of the HomekitCharacteristic, in accordance with the Homekit HomekitAccessory
     * Protocol.
     *
     * @return the resulting JSON object
     */
    JsonObject toJson();

    /**
     * Creates a reduced JSON representation of the HomekitCharacteristic, excluding some optional fields.
     *
     * @return the resulting JSON object
     */
    JsonObject toReducedJson();

    /**
     * Creates the JSON representation of an event for the HomekitCharacteristic, in accordance with the Homekit HomekitAccessory
     * Protocol.
     *
     * @return the resulting JSON object
     */
    JsonObject toEventJson();

    /**
     * Creates the JSON representation of an event for the HomekitCharacteristic with a specific value.
     *
     * @param value the value to include in the event
     * @return the resulting JSON object
     */
    JsonObject toEventJson(T value);

    JsonValue toValueJson(T value);

    JsonValue toValueJson(State state);

    /**
     * Gets the current value of the characteristic.
     *
     * @return the current value
     */
    T getValue();

    /**
     * Sets the value of the characteristic.
     *
     * @param value the new value to set
     */
    void setValue(T value) throws Exception;;

    /**
     * Sets the value of the characteristic from a JSON value.
     *
     * @param jsonValue the JSON value to convert and set
     * @throws Exception if the value cannot be set
     */
    void setValue(JsonValue jsonValue) throws Exception;

    /**
     * Converts a JSON value to the characteristic's type.
     *
     * @param jsonValue the JSON value to convert
     * @return the converted value
     */
    T toValue(JsonValue jsonValue);

    /**
     * Converts a State to the characteristic's type.
     *
     * @param state the state to convert
     * @return the converted value
     */
    T toValue(State state);

    /**
     * Converts the characteristic's type to a State.
     *
     * @param value the value to convert
     * @return the converted state
     */
    State toState(T value);

    State toState(JsonValue jsonValue);

    /**
     * Gets the default value for the characteristic.
     *
     * @return the default value
     */
    T getDefault();

    // String getAcceptedItemType();
    //
    // ChannelTypeUID getChannelTypeUID();

    String getDescription();

    void updateWith(HomekitCharacteristic<?> other);
}
