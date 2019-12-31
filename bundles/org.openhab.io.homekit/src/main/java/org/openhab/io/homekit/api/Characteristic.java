package org.openhab.io.homekit.api;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.openhab.core.common.registry.Identifiable;
import org.openhab.core.items.StateChangeListener;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.types.State;
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
public interface Characteristic<T> extends Identifiable<CharacteristicUID>, StateChangeListener {

    /**
     * Characteristic Instance IDs are assigned from the same number pool that is unique within each
     * Accessory. For example, if the first Characteristic has an Instance Id of “1”, then
     * no other Characteristic can have an Instance Id of “1” within the parent Accessory object.
     * After a firmware update, Characteristic types that remain unchanged must retain their previous instance
     * Ids, newly added Characteristic must not reuse Instance Ids from Characteristics that were removed
     * in the firmware update.
     *
     * @return the unique identifier.
     */
    long getId();

    boolean isType(String aType);

    String getInstanceType();

    Service getService();

    void setChannelUID(ChannelUID channelUID);

    ChannelUID getChannelUID();

    /**
     * Services may specify the Characteristics that are to be hidden
     *
     * @return true if the Characteristic is hidden
     */
    boolean isHidden();

    /**
     * Retrieves the current value of the characteristic // could come from openHAB
     *
     * @return a the current value.
     */

    T getValue() throws Exception;

    /**
     * Update the characteristic value using a new value
     *
     * @param value the new value to set.
     * @throws Exception if the value cannot be set.
     */
    void setValue(T value) throws Exception;

    /**
     * Update the characteristic value using a new value that is encoded as a JsonValue
     *
     * @param jsonValue the JSON serialized value to set.
     * @throws Exception
     */
    void setValue(JsonValue jsonValue) throws Exception;

    /**
     * Creates the JSON representation of the Characteristic, in accordance with the Homekit Accessory
     * Protocol.
     *
     * @return the resulting JSON object
     */
    JsonObject toJson();

    JsonObject toReducedJson();

    /**
     * Creates the JSON representation of an event for the Characteristic, in accordance with the Homekit Accessory
     * Protocol.
     *
     * @return the resulting JSON object
     */
    JsonObject toEventJson();

    JsonObject toJson(boolean includeMeta, boolean includePermissions, boolean includeType, boolean includeEvent);

    void setEventsEnabled(boolean value);

    JsonObject toEventJson(State state);
}
