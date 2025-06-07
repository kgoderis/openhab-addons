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

package org.openhab.io.homekit.core.server;

import java.util.Objects;

import org.openhab.io.homekit.api.event.HomekitEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents the state of a HomeKit accessory server.
 *
 * <p>
 * This class implements an enum-like pattern to represent various states of a HomeKit accessory server,
 * including connection states, pairing states, and operational states. Each state includes:
 * </p>
 * <ul>
 * <li>A name for identification</li>
 * <li>A human-readable description</li>
 * <li>An associated event type</li>
 * <li>A flag indicating if the state is transient</li>
 * </ul>
 *
 * <p>
 * Key responsibilities:
 * </p>
 * <ul>
 * <li>Defining server states and their properties</li>
 * <li>Managing state transitions and validation</li>
 * <li>Providing state information for event handling</li>
 * <li>Supporting server lifecycle management</li>
 * </ul>
 *
 * <p>
 * The class integrates with:
 * </p>
 * <ul>
 * <li>{@link HomekitEventType} for event type mapping</li>
 * <li>{@link org.openhab.io.homekit.api.server.HomekitAccessoryServer} for server state management</li>
 * <li>{@link org.openhab.io.homekit.api.event.HomekitEvent OpenHAB's event system} for state change notifications</li>
 * </ul>
 *
 * @author Karel Goderis - Initial contribution
 * @version 1.0
 * @since 1.0
 */
public class HomekitAccessoryServerState {
    // ========== Log Message Prefixes ==========
    protected static final String LOG_PREFIX = "Homekit ServerState: ";
    protected static final String LOG_STATE = LOG_PREFIX + "State - ";
    protected static final String LOG_TRANSITION = LOG_PREFIX + "Transition - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";

    private final Logger logger = LoggerFactory.getLogger(HomekitAccessoryServerState.class);
    private final String name;
    private final String description;
    private final HomekitEventType eventType;
    private final boolean isTransient;

    /**
     * Creates a new server state with the specified properties.
     *
     * <p>
     * This constructor builds a complete server state instance with all required properties.
     * The state is used to track the current operational status of a HomeKit accessory server.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Validates all input parameters</li>
     * <li>Initializes all internal fields</li>
     * <li>Sets up state properties</li>
     * <li>Logs state creation for debugging</li>
     * </ul>
     *
     * @param name The state name, used for identification and logging
     * @param description A human-readable description of the state's meaning
     * @param eventType The associated event type for state change notifications
     * @param isTransient Whether this state is temporary and expected to change
     * @throws IllegalArgumentException if any parameter is null or empty
     */
    private HomekitAccessoryServerState(String name, String description, HomekitEventType eventType,
            boolean isTransient) {
        if (name == null || name.isEmpty()) {
            logger.error("{}State name cannot be null or empty", LOG_ERROR);
            throw new IllegalArgumentException("State name cannot be null or empty");
        }
        if (description == null || description.isEmpty()) {
            logger.error("{}State description cannot be null or empty", LOG_ERROR);
            throw new IllegalArgumentException("State description cannot be null or empty");
        }
        if (eventType == null) {
            logger.error("{}Event type cannot be null", LOG_ERROR);
            throw new IllegalArgumentException("Event type cannot be null");
        }

        this.name = name;
        this.description = description;
        this.eventType = eventType;
        this.isTransient = isTransient;
        logger.trace("{}Created new server state: {} ({})", LOG_STATE, name, description);
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
     * <p>
     * The name is used for identification and logging purposes.
     * It should be unique within the set of possible server states.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Returns the internal name field</li>
     * <li>Used for state identification</li>
     * <li>Supports logging and debugging</li>
     * <li>Provides trace-level logging</li>
     * </ul>
     *
     * @return The state name as a string
     */
    public String getName() {
        logger.trace("{}Getting state name: {}", LOG_STATE, name);
        return name;
    }

    /**
     * Gets the description of this state.
     *
     * <p>
     * The description provides a human-readable explanation of what the state means
     * and its implications for the server's operation.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Returns the internal description field</li>
     * <li>Used for user interface display</li>
     * <li>Supports state understanding</li>
     * <li>Provides trace-level logging</li>
     * </ul>
     *
     * @return A human-readable description of the state
     */
    public String getDescription() {
        logger.trace("{}Getting state description: {}", LOG_STATE, description);
        return description;
    }

    /**
     * Gets the event type associated with this state.
     *
     * <p>
     * The event type is used to notify interested parties when the server
     * transitions to this state. It helps coordinate state changes across
     * the HomeKit integration.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Returns the internal event type field</li>
     * <li>Used for event handling</li>
     * <li>Supports state change notifications</li>
     * <li>Provides trace-level logging</li>
     * </ul>
     *
     * @return The associated HomekitEventType for state change notifications
     */
    public HomekitEventType getEventType() {
        logger.trace("{}Getting event type for state {}: {}", LOG_STATE, name, eventType);
        return eventType;
    }

    /**
     * Checks if this state is transient.
     *
     * <p>
     * A transient state is one that is expected to change quickly, such as
     * authentication or initialization states. Non-transient states represent
     * more stable operational conditions.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Returns the internal transient flag</li>
     * <li>Used for state transition handling</li>
     * <li>Supports state stability checks</li>
     * <li>Provides trace-level logging</li>
     * </ul>
     *
     * @return true if this state is temporary, false if it represents a stable condition
     */
    public boolean isTransient() {
        logger.trace("{}Checking if state {} is transient: {}", LOG_STATE, name, isTransient);
        return isTransient;
    }

    /**
     * Returns a string representation of this state.
     *
     * <p>
     * The string format combines the state name and description for easy
     * identification and debugging purposes.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Uses String.format for consistent formatting</li>
     * <li>Combines name and description</li>
     * <li>Supports debugging and logging</li>
     * <li>Provides trace-level logging</li>
     * </ul>
     *
     * @return A string in the format "{name} ({description})"
     */
    @Override
    public String toString() {
        String result = String.format("%s (%s)", name, description);
        logger.trace("{}Converting state to string: {}", LOG_STATE, result);
        return result;
    }

    /**
     * Compares this state with another object for equality.
     *
     * <p>
     * Two states are considered equal if they have the same name, description,
     * event type, and transient status.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Implements standard equals contract</li>
     * <li>Compares all relevant fields</li>
     * <li>Handles null cases properly</li>
     * <li>Provides trace-level logging</li>
     * </ul>
     *
     * @param obj The object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        logger.trace("{}Comparing state {} with object: {}", LOG_STATE, name, obj);
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        HomekitAccessoryServerState other = (HomekitAccessoryServerState) obj;
        boolean result = name.equals(other.name) && description.equals(other.description)
                && eventType == other.eventType && isTransient == other.isTransient;
        logger.trace("{}Equality comparison result: {}", LOG_STATE, result);
        return result;
    }

    /**
     * Generates a hash code for this state.
     *
     * <p>
     * The hash code is based on all fields that are used in equals comparison.
     * This ensures consistency between equals and hashCode.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Uses Objects.hash for consistent hashing</li>
     * <li>Includes all relevant fields</li>
     * <li>Maintains equals/hashCode contract</li>
     * <li>Provides trace-level logging</li>
     * </ul>
     *
     * @return A hash code value for this state
     */
    @Override
    public int hashCode() {
        int result = Objects.hash(name, description, eventType, isTransient);
        logger.trace("{}Generated hash code for state {}: {}", LOG_STATE, name, result);
        return result;
    }
}
