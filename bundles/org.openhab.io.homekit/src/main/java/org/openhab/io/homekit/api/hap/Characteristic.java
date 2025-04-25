package org.openhab.io.homekit.api.hap;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.Identifiable;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.listener.CharacteristicChangeListener;
import org.openhab.io.homekit.internal.characteristic.CharacteristicUID;

/**
 * Interface for the Characteristics provided by a Service.
 *
 * <p>
 * Characteristics are the lowest level building block of the Homekit Accessory Protocol. They
 * define variables that can be retrieved or set by the remote client.
 *
 * @author Andy Lintner
 */
@NonNullByDefault
public interface Characteristic<T> extends Identifiable<CharacteristicUID>, Comparable<Characteristic<?>> {

    /**
     * Characteristic Instance IDs are assigned from the same number pool that is unique within each
     * Accessory. For example, if the first Characteristic has an Instance Id of "1", then
     * no other Characteristic can have an Instance Id of "1" within the parent Accessory object.
     * After a firmware update, Characteristic types that remain unchanged must retain their previous instance
     * Ids, newly added Characteristic must not reuse Instance Ids from Characteristics that were removed
     * in the firmware update.
     *
     * @return the unique identifier.
     */
    long getInstanceId();

    /**
     * Gets the parent Service that contains this Characteristic.
     *
     * @return the parent Service
     */
    Service getService();

    /**
     * Checks if this Characteristic is of the specified type.
     *
     * @param aType the type to check against
     * @return true if the Characteristic is of the specified type
     */
    boolean isType(String aType);

    // Static method can't be called through instance
    static String getType() {
        throw new UnsupportedOperationException("Must be implemented by characteristic class");
    }

    /**
     * Gets the instance type of this Characteristic.
     *
     * @return the instance type
     */
    String getInstanceType();

    /**
     * Services may specify the Characteristics that are to be hidden
     *
     * @return true if the Characteristic is hidden
     */
    boolean isHidden();

    /**
     * Sets whether events are enabled for this Characteristic.
     *
     * @param value true to enable events, false to disable
     */
    void setHasEvents(boolean value);

    /**
     * Adds a listener to be notified of characteristic changes.
     *
     * @param listener the listener to add
     */
    void addChangeListener(CharacteristicChangeListener listener);

    /**
     * Removes a listener from being notified of characteristic changes.
     *
     * @param listener the listener to remove
     */
    void removeChangeListener(CharacteristicChangeListener listener);

    /**
     * Creates a JSON representation of the Characteristic with specified fields included.
     *
     * @param includeMeta whether to include metadata (iid, aid, type)
     * @param includePermissions whether to include permissions
     * @param includeType whether to include type information
     * @param includeEvent whether to include event information
     * @return the resulting JSON object
     */
    JsonObject toJson(boolean includeMeta, boolean includePermissions, boolean includeType, boolean includeEvent);

    /**
     * Creates the JSON representation of the Characteristic, in accordance with the Homekit Accessory
     * Protocol.
     *
     * @return the resulting JSON object
     */
    JsonObject toJson();

    /**
     * Creates a reduced JSON representation of the Characteristic, excluding some optional fields.
     *
     * @return the resulting JSON object
     */
    JsonObject toReducedJson();

    /**
     * Creates the JSON representation of an event for the Characteristic, in accordance with the Homekit Accessory
     * Protocol.
     *
     * @return the resulting JSON object
     */
    JsonObject toEventJson();

    /**
     * Creates the JSON representation of an event for the Characteristic with a specific value.
     *
     * @param value the value to include in the event
     * @return the resulting JSON object
     */
    JsonObject toEventJson(T value);

    JsonValue toValueJson(T value);

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

    void updateWith(Characteristic<?> other);
}
