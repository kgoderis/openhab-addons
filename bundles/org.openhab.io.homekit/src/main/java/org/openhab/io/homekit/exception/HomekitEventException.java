package org.openhab.io.homekit.exception;

/**
 * Exception thrown when there is an error during HomeKit event handling.
 * 
 * This exception is used to indicate problems that occur during the processing
 * of HomeKit events, such as event delivery failures, event handling errors,
 * or event state management issues. It extends {@link HomekitServerException}
 * to provide specific error handling for event-related operations.
 * 
 * Common scenarios where this exception is thrown:
 * - Event delivery failures
 * - Event handler errors
 * - Event state management issues
 * - Event queue processing errors
 * - Event subscription failures
 * - Event notification errors
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0
 */
public class HomekitEventException extends HomekitServerException {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new HomeKit event exception with the specified detail message.
     *
     * @param message the detail message (which is saved for later retrieval by the
     *            {@link #getMessage()} method)
     */
    public HomekitEventException(String message) {
        super(message);
    }

    /**
     * Constructs a new HomeKit event exception with the specified detail message and cause.
     *
     * @param message the detail message (which is saved for later retrieval by the
     *            {@link #getMessage()} method)
     * @param cause the cause (which is saved for later retrieval by the
     *            {@link #getCause()} method)
     */
    public HomekitEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
