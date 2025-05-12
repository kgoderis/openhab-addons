package org.openhab.io.homekit.core.accessory;

import org.openhab.io.homekit.api.event.HomekitEventType;

/**
 * Extended version of accessory state using the Enum extension pattern.
 * This class provides additional functionality and state management for accessories.
 */
public class HomekitAccessoryServerState {
    private final String name;
    private final String description;
    private final HomekitEventType eventType;
    private final boolean isTransient;

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

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public HomekitEventType getEventType() {
        return eventType;
    }

    public boolean isTransient() {
        return isTransient;
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", name, description);
    }

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

    @Override
    public int hashCode() {
        return 31 * name.hashCode() + eventType.hashCode();
    }
}
