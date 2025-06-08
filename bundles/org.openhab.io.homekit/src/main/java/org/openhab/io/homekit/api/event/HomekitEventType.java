/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.openhab.io.homekit.api.event;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Enum representing the different types of events in the HomeKit integration.
 *
 * This enum defines all possible event types that can occur in the HomeKit integration,
 * categorizing them by their source and purpose. Events are used to communicate state
 * changes, lifecycle events, and other important notifications throughout the system.
 *
 * The event types are organized into categories:
 * - Characteristic events: Value changes and state updates for characteristics
 * - Service events: Service lifecycle and state changes
 * - Accessory events: Accessory lifecycle and state changes
 * - Server events: Server state and connection changes
 * - Pairing events: Pairing state changes
 * - Subscription events: Subscription lifecycle events
 *
 * Key implementation details:
 * - Hierarchical event matching
 * - Wildcard support for event categories
 * - Thread-safe event type comparison
 * - Extensible event type system
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0.0
 */
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

    /**
     * Checks if this event type matches another event type.
     * This method implements a hierarchical matching system where:
     * - ANY matches all event types
     * - Category ANY (e.g., CHARACTERISTIC_ANY) matches all events in that category
     * - Exact matches are always true
     *
     * @param other the event type to compare against
     * @return true if this event type matches the other event type
     * @since 1.0.0
     */
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
