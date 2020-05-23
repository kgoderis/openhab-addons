package org.openhab.io.homekit.api;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.openhab.core.thing.ChannelUID;
import org.openhab.core.types.State;

/**
 * Interface for the Characteristics provided by a Service.
 *
 * <p>
 * Characteristics are the lowest level building block of the Homekit Accessory Protocol. They
 * define variables that can be retrieved or set by the remote client.
 *
 * @author Andy Lintner
 */
public interface ManagedCharacteristic<T> extends Characteristic {

    void setChannelUID(ChannelUID channelUID);

    ChannelUID getChannelUID();

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

    JsonObject toEventJson(State state);

    // /**
    // * Converts from the JSON value to a Java object of the type T
    // *
    // * @param jsonValue the JSON value to convert from.
    // * @return the converted Java object.
    // */
    // T convert(JsonValue jsonValue);
    //
    // T convert(State state);
    //
    // State convert(T value);
    //
    // /**
    // * Provide a default value for the characteristic to be send when the real value cannot be retrieved.
    // *
    // * @return a sensible default value.
    // */
    // T getDefault();
}
