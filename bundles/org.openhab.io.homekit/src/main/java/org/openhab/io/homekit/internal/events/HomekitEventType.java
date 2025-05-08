package org.openhab.io.homekit.internal.events;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public enum HomekitEventType {
    // HomekitCharacteristic events
    CHARACTERISTIC_VALUE_CHANGED,
    CHARACTERISTIC_STATE_CHANGED,
    CHARACTERISTIC_ADDED,
    CHARACTERISTIC_REMOVED,
    CHARACTERISTIC_START_EVENTS,
    CHARACTERISTIC_STOP_EVENTS,
    CHARACTERISTIC_ANY, // Matches any characteristic event
    CHARACTERISTIC_CHANGE_VALUE,

    // HomekitService events
    SERVICE_STATE_CHANGED,
    SERVICE_ADDED,
    SERVICE_REMOVED,
    SERVICE_ANY, // Matches any service event

    // HomekitAccessory events
    ACCESSORY_STATE_CHANGED,
    ACCESSORY_ADDED,
    ACCESSORY_REMOVED,
    ACCESSORY_UID_CHANGED,
    ACCESSORY_ANY, // Matches any accessory event

    // Server events
    SERVER_STATE_CHANGED,
    SERVER_STATE_CONFIGURATION_NUMBER_CHANGED,
    SERVER_STATE_CONNECTED,
    SERVER_STATE_DISCONNECTED,
    SERVER_STATE_PAIRED,
    SERVER_STATE_PAIR_VERIFIED,
    SERVER_STATE_PAIR_UNVERIFIED,
    SERVER_STATE_UNPAIRED,
    SERVER_STATE_PAIRING_MISSING,
    SERVER_STATE_MISSING_SETUP_CODE,
    SERVER_ANY, // Matches any server event

    // HomekitPairing events
    PAIRING_STATE_CHANGED,
    PAIRING_ANY, // Matches any pairing event

    // Subscription events
    SUBSCRIPTION_ADDED,
    SUBSCRIPTION_REMOVED,
    SUBSCRIPTION_FAILED,
    SUBSCRIPTION_ORPHANED,
    SUBSCRIPTION_ANY, // Matches any subscription event

    // Wildcard for all events
    ANY; // Matches any event type

    public boolean matches(HomekitEventType other) {
        if (this == ANY || other == ANY) {
            return true;
        }
        if (this == other) {
            return true;
        }
        return switch (this) {
            case CHARACTERISTIC_ANY -> other.name().startsWith("CHARACTERISTIC_");
            case SERVICE_ANY -> other.name().startsWith("SERVICE_");
            case ACCESSORY_ANY -> other.name().startsWith("ACCESSORY_");
            case SERVER_ANY -> other.name().startsWith("SERVER_");
            case PAIRING_ANY -> other.name().startsWith("PAIRING_");
            case SUBSCRIPTION_ANY -> other.name().startsWith("SUBSCRIPTION_");
            default -> false;
        };
    }
}
