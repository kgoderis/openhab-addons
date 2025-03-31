package org.openhab.io.homekit.api;

import javax.json.JsonObject;

import org.openhab.core.common.registry.Identifiable;
import org.openhab.io.homekit.internal.characteristic.CharacteristicUID;
import org.openhab.io.homekit.internal.listeners.CharacteristicChangeListener;

/**
 * Interface for the Characteristics provided by a Service.
 *
 * <p>
 * Characteristics are the lowest level building block of the Homekit Accessory Protocol. They
 * define variables that can be retrieved or set by the remote client.
 *
 * @author Andy Lintner
 */
public interface Characteristic extends Identifiable<CharacteristicUID> {

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
    long getId();

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
    void setEventsEnabled(boolean value);

    /**
     * Adds a listener to be notified of characteristic changes.
     *
     * @param listener the listener to add
     */
    void addListener(CharacteristicChangeListener listener);

    /**
     * Removes a listener from being notified of characteristic changes.
     *
     * @param listener the listener to remove
     */
    void removeListener(CharacteristicChangeListener listener);

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

    // String getAcceptedItemType();
    //
    // ChannelTypeUID getChannelTypeUID();
}
