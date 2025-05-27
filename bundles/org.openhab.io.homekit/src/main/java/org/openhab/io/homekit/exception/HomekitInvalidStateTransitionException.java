package org.openhab.io.homekit.exception;

import org.openhab.io.homekit.core.server.HomekitAccessoryServerState;

/**
 * Exception thrown when an invalid state transition is attempted in a HomeKit server.
 * 
 * This exception is used to indicate that a transition from one server state to another
 * is not allowed, typically due to protocol or lifecycle constraints. It extends
 * {@link HomekitServerException} to provide specific error handling for state transition issues.
 * 
 * Common scenarios where this exception is thrown:
 * - Attempting to move to an invalid or unsupported server state
 * - Violating the expected server state machine
 * - Protocol errors related to state transitions
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0
 */
public class HomekitInvalidStateTransitionException extends HomekitServerException {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new HomeKit invalid state transition exception for the given states.
     *
     * @param current the current server state
     * @param next the attempted next server state
     */
    public HomekitInvalidStateTransitionException(HomekitAccessoryServerState current,
            HomekitAccessoryServerState next) {
        super(String.format("Invalid state transition from %s to %s", current, next));
    }

    /**
     * Constructs a new HomeKit invalid state transition exception with the specified detail message.
     *
     * @param message the detail message (which is saved for later retrieval by the
     *            {@link #getMessage()} method)
     */
    public HomekitInvalidStateTransitionException(String message) {
        super(message);
    }
}
