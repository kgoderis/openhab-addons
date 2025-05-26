package org.openhab.io.homekit.core.accessory;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a persisted HomeKit accessory in storage.
 * 
 * <p>
 * This class is used to store and retrieve accessory data from persistent storage.
 * It maintains the JSON representation of the accessory and its type information.
 * </p>
 *
 * <p>
 * The class integrates with:
 * <ul>
 *   <li>{@link org.openhab.io.homekit.api.accessory.HomekitAccessory} for accessory data storage</li>
 *   <li>{@link org.openhab.io.homekit.api.factory.HomekitAccessoryFactory} for accessory creation</li>
 * </ul>
 * </p>
 *
 * @author Karel Goderis - Initial Contribution
 * @since 1.0
 */
@NonNullByDefault
public class HomekitPersistedAccessory {
    // ========== Log Message Prefixes ==========
    private static final String LOG_PREFIX = "Homekit PersistedAccessory: ";
    private static final String LOG_INIT = LOG_PREFIX + "Init - ";
    private static final String LOG_STATE = LOG_PREFIX + "State - ";
    private static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    private static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    private static final String LOG_WARN = LOG_PREFIX + "Warning - ";

    private final Logger logger = LoggerFactory.getLogger(HomekitPersistedAccessory.class);

    private String json;
    // private String instanceIdPool;
    private String accessoryType;

    /**
     * Creates a new empty persisted accessory.
     * 
     * <p>
     * Initializes all fields with empty values.
     * </p>
     */
    public HomekitPersistedAccessory() {
        json = "";
        // instanceIdPool = "";
        accessoryType = "";
        logger.debug("{}Created new empty persisted accessory", LOG_INIT);
    }

    /**
     * Creates a new persisted accessory with the specified type and JSON data.
     *
     * @param accessoryType The type identifier of the accessory
     * @param json The JSON representation of the accessory
     */
    public HomekitPersistedAccessory(String accessoryType, String json) {
        this.json = json;
        // this.instanceIdPool = Long.toString(instanceIdPool);
        this.accessoryType = accessoryType;
        logger.debug("{}Created new persisted accessory of type {}", LOG_INIT, accessoryType);
    }

    /**
     * Gets the JSON representation of the accessory.
     *
     * @return The JSON string containing the accessory data
     */
    public String getJson() {
        return json;
    }

    // public long getInstanceIdPool() {
    // return Long.parseLong(instanceIdPool);
    // }

    /**
     * Gets the type identifier of the accessory.
     *
     * @return The accessory type string
     */
    public String getAccessoryType() {
        return accessoryType;
    }

    /**
     * Sets the JSON representation of the accessory.
     *
     * @param json The JSON string containing the accessory data
     */
    public void setJson(String json) {
        this.json = json;
        logger.debug("{}Updated JSON data for accessory type {}", LOG_STATE, accessoryType);
    }

    // public void setInstanceIdPool(long instanceIdPool) {
    // this.instanceIdPool = Long.toString(instanceIdPool);
    // }

    // public void setServerUID(HomekitAccessoryServerUID serverUID) {
    // this.serverUID = serverUID.toString();
    // }

    /**
     * Sets the type identifier of the accessory.
     *
     * @param accessoryType The accessory type string
     */
    public void setAccessoryType(String accessoryType) {
        this.accessoryType = accessoryType;
        logger.debug("{}Updated accessory type to {}", LOG_STATE, accessoryType);
    }
}
