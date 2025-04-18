package org.openhab.io.homekit.internal.accessory;

import org.openhab.io.homekit.internal.events.AccessoryServerEvent.AccessoryServerEventType;

/**
 * Extended version of accessory state using the Enum extension pattern.
 * This class provides additional functionality and state management for accessories.
 */
public class AccessoryServerState {
    private final String name;
    private final String description;
    private final AccessoryServerEventType eventType;
    private final boolean isTransient;

    private AccessoryServerState(String name, String description, AccessoryServerEventType eventType,
            boolean isTransient) {
        this.name = name;
        this.description = description;
        this.eventType = eventType;
        this.isTransient = isTransient;
    }

    public static final AccessoryServerState UNKNOWN = new AccessoryServerState("UNKNOWN", "Unknown state",
            AccessoryServerEventType.SERVER_UPDATED, false);

    public static final AccessoryServerState CONNECTED = new AccessoryServerState("CONNECTED",
            "Connected to controller", AccessoryServerEventType.SERVER_STATE_CONNECTED, false);

    public static final AccessoryServerState DISCONNECTED = new AccessoryServerState("DISCONNECTED",
            "Disconnected from controller", AccessoryServerEventType.SERVER_STATE_DISCONNECTED, false);

    public static final AccessoryServerState PAIRED = new AccessoryServerState("PAIRED", "Paired with controller",
            AccessoryServerEventType.SERVER_STATE_PAIRED, false);

    public static final AccessoryServerState PAIR_VERIFIED = new AccessoryServerState("PAIR_VERIFIED",
            "Pairing verified with controller", AccessoryServerEventType.SERVER_STATE_PAIR_VERIFIED, false);

    public static final AccessoryServerState PAIR_UNVERIFIED = new AccessoryServerState("PAIR_UNVERIFIED",
            "Pairing exists but not verified", AccessoryServerEventType.SERVER_STATE_PAIR_UNVERIFIED, false);

    public static final AccessoryServerState UNPAIRED = new AccessoryServerState("UNPAIRED",
            "Not paired with any controller", AccessoryServerEventType.SERVER_STATE_UNPAIRED, false);

    public static final AccessoryServerState PAIRED_TO_OTHER_CONTROLLER = new AccessoryServerState(
            "PAIRED_TO_OTHER_CONTROLLER", "Paired with another controller",
            AccessoryServerEventType.SERVER_STATE_PAIRED, false);

    public static final AccessoryServerState PAIRING_MISSING = new AccessoryServerState("PAIRING_MISSING",
            "Pairing information is missing", AccessoryServerEventType.SERVER_STATE_PAIRING_MISSING, false);

    public static final AccessoryServerState MISSING_SETUP_CODE = new AccessoryServerState("MISSING_SETUP_CODE",
            "Setup code is missing or invalid", AccessoryServerEventType.SERVER_STATE_MISSING_SETUP_CODE, false);

    public static final AccessoryServerState AUTHENTICATING = new AccessoryServerState("AUTHENTICATING",
            "Authenticating with controller", AccessoryServerEventType.SERVER_UPDATED, true);

    public static final AccessoryServerState READY = new AccessoryServerState("READY", "Ready for operation",
            AccessoryServerEventType.SERVER_UPDATED, false);

    public static final AccessoryServerState STOPPED = new AccessoryServerState("STOPPED", "Server stopped",
            AccessoryServerEventType.SERVER_UPDATED, false);

    public static final AccessoryServerState RESET = new AccessoryServerState("RESET",
            "Accessory has been factory reset", AccessoryServerEventType.SERVER_UPDATED, false);

    public static final AccessoryServerState PAIR_SETUP_INITIAL = new AccessoryServerState("PAIR_SETUP_INITIAL",
            "Starting pair setup process", AccessoryServerEventType.SERVER_UPDATED, true);

    public static final AccessoryServerState PAIR_SETUP_SRP = new AccessoryServerState("PAIR_SETUP_SRP",
            "Executing SRP protocol exchange", AccessoryServerEventType.SERVER_UPDATED, true);

    public static final AccessoryServerState PAIR_SETUP_VERIFY = new AccessoryServerState("PAIR_SETUP_VERIFY",
            "Verifying pairing proof", AccessoryServerEventType.SERVER_UPDATED, true);

    public static final AccessoryServerState PAIR_SETUP_EXCHANGE = new AccessoryServerState("PAIR_SETUP_EXCHANGE",
            "Exchanging encryption keys", AccessoryServerEventType.SERVER_UPDATED, true);

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public AccessoryServerEventType getEventType() {
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
        AccessoryServerState other = (AccessoryServerState) obj;
        return name.equals(other.name) && eventType == other.eventType;
    }

    @Override
    public int hashCode() {
        return 31 * name.hashCode() + eventType.hashCode();
    }
}
