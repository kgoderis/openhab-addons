package org.openhab.io.homekit.api;

import javax.json.JsonObject;

/**
 * Interface for the Characteristics provided by a Service.
 *
 * <p>
 * Characteristics are the lowest level building block of the Homekit Accessory Protocol. They
 * define variables that can be retrieved or set by the remote client.
 *
 * @author Andy Lintner
 */
public interface Characteristic {

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

    Service getService();

    boolean isType(String aType);

    String getInstanceType();

    /**
     * Services may specify the Characteristics that are to be hidden
     *
     * @return true if the Characteristic is hidden
     */
    boolean isHidden();

    void setEventsEnabled(boolean value);

    JsonObject toJson(boolean includeMeta, boolean includePermissions, boolean includeType, boolean includeEvent);

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

}
