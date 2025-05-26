package org.openhab.io.homekit.core.server;

import org.openhab.io.homekit.api.event.HomekitEventType;

/**
 * Represents the state of a HomeKit accessory server.
 *
 * <p>
 * This class implements an enum-like pattern to represent various states of a HomeKit accessory server,
 * including connection states, pairing states, and operational states. Each state includes:
 * <ul>
 *   <li>A name for identification</li>
 *   <li>A human-readable description</li>
 *   <li>An associated event type</li>
 *   <li>A flag indicating if the state is transient</li>
 * </ul>
 * </p>
 *
 * <p>
 * The class integrates with:
 * <ul>
 *   <li>{@link HomekitEventType} for event type mapping</li>
 *   <li>{@link org.openhab.io.homekit.api.server.HomekitAccessoryServer} for server state management</li>
 * </ul>
 * </p>
 *
 * @author Karel Goderis - Initial contribution
 * @version 1.0
 * @since 1.0
 */
public class HomekitAccessoryServerState {
    private final String name;
    private final String description;
    private final HomekitEventType eventType;
    private final boolean isTransient;

    /**
     * Creates a new server state with the specified properties.
     *
     * @param name The state name
     * @param description A human-readable description of the state
     * @param eventType The associated event type
     * @param isTransient Whether this state is temporary
     */
    private HomekitAccessoryServerState(String name, String description, HomekitEventType eventType,
            boolean isTransient) {
        this.name = name;
        this.description = description;
        this.eventType = eventType;
        this.isTransient = isTransient;
    }

    public static final HomekitAccessoryServerState UNKNOWN = new HomekitAccessoryServerState("UNKNOWN",
            "Unknown state", HomekitEventType.SERVER_STATE_CONNECTED, false);

    public static final HomekitAccessoryServerState CONNECTED = new HomekitAccessoryServerState("CONNECTED",
            "Connected to controller", HomekitEventType.SERVER_STATE_CONNECTED, false);

    public static final HomekitAccessoryServerState DISCONNECTED = new HomekitAccessoryServerState("DISCONNECTED",
            "Disconnected from controller", HomekitEventType.SERVER_STATE_DISCONNECTED, false);

    public static final HomekitAccessoryServerState PAIRED = new HomekitAccessoryServerState("PAIRED",
            "Paired with controller", HomekitEventType.SERVER_STATE_PAIRED, false);

    public static final HomekitAccessoryServerState PAIR_VERIFIED = new HomekitAccessoryServerState("PAIR_VERIFIED",
            "HomekitPairing verified with controller", HomekitEventType.SERVER_STATE_PAIR_VERIFIED, false);

    public static final HomekitAccessoryServerState PAIR_UNVERIFIED = new HomekitAccessoryServerState("PAIR_UNVERIFIED",
            "HomekitPairing exists but not verified", HomekitEventType.SERVER_STATE_PAIR_UNVERIFIED, false);

    public static final HomekitAccessoryServerState UNPAIRED = new HomekitAccessoryServerState("UNPAIRED",
            "Not paired with any controller", HomekitEventType.SERVER_STATE_UNPAIRED, false);

    public static final HomekitAccessoryServerState PAIRED_TO_OTHER_CONTROLLER = new HomekitAccessoryServerState(
            "PAIRED_TO_OTHER_CONTROLLER", "Paired with another controller", HomekitEventType.SERVER_STATE_PAIRED,
            false);

    public static final HomekitAccessoryServerState PAIRING_MISSING = new HomekitAccessoryServerState("PAIRING_MISSING",
            "HomekitPairing information is missing", HomekitEventType.SERVER_STATE_PAIRING_MISSING, false);

    public static final HomekitAccessoryServerState MISSING_SETUP_CODE = new HomekitAccessoryServerState(
            "MISSING_SETUP_CODE", "Setup code is missing or invalid", HomekitEventType.SERVER_STATE_MISSING_SETUP_CODE,
            false);

    public static final HomekitAccessoryServerState AUTHENTICATING = new HomekitAccessoryServerState("AUTHENTICATING",
            "Authenticating with controller", HomekitEventType.SERVER_STATE_CONNECTED, true);

    public static final HomekitAccessoryServerState READY = new HomekitAccessoryServerState("READY",
            "Ready for operation", HomekitEventType.SERVER_STATE_CONNECTED, false);

    public static final HomekitAccessoryServerState STOPPED = new HomekitAccessoryServerState("STOPPED",
            "Server stopped", HomekitEventType.SERVER_STATE_CONNECTED, false);

    public static final HomekitAccessoryServerState RESET = new HomekitAccessoryServerState("RESET",
            "HomekitAccessory has been factory reset", HomekitEventType.SERVER_STATE_CONNECTED, false);

    public static final HomekitAccessoryServerState PAIR_SETUP_INITIAL = new HomekitAccessoryServerState(
            "PAIR_SETUP_INITIAL", "Starting pair setup process", HomekitEventType.SERVER_STATE_CONNECTED, true);

    public static final HomekitAccessoryServerState PAIR_SETUP_SRP = new HomekitAccessoryServerState("PAIR_SETUP_SRP",
            "Executing SRP protocol exchange", HomekitEventType.SERVER_STATE_CONNECTED, true);

    public static final HomekitAccessoryServerState PAIR_SETUP_VERIFY = new HomekitAccessoryServerState(
            "PAIR_SETUP_VERIFY", "Verifying pairing proof", HomekitEventType.SERVER_STATE_CONNECTED, true);

    public static final HomekitAccessoryServerState PAIR_SETUP_EXCHANGE = new HomekitAccessoryServerState(
            "PAIR_SETUP_EXCHANGE", "Exchanging encryption keys", HomekitEventType.SERVER_STATE_CONNECTED, true);

    /**
     * Gets the name of this state.
     *
     * @return The state name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the description of this state.
     *
     * @return A human-readable description of the state
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the event type associated with this state.
     *
     * @return The associated HomekitEventType
     */
    public HomekitEventType getEventType() {
        return eventType;
    }

    /**
     * Checks if this state is transient.
     *
     * @return true if this state is temporary, false otherwise
     */
    public boolean isTransient() {
        return isTransient;
    }

    /**
     * Returns a string representation of this state.
     * The format is "{name} ({description})".
     *
     * @return A string representation of this state
     */
    @Override
    public String toString() {
        return String.format("%s (%s)", name, description);
    }

    /**
     * Compares this state with another object for equality.
     * Two states are equal if they have the same name and event type.
     *
     * @param obj The object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        HomekitAccessoryServerState other = (HomekitAccessoryServerState) obj;
        return name.equals(other.name) && eventType == other.eventType;
    }

    /**
     * Returns a hash code for this state.
     * The hash code is based on the state name and event type.
     *
     * @return A hash code for this state
     */
    @Override
    public int hashCode() {
        return 31 * name.hashCode() + eventType.hashCode();
    }
}
