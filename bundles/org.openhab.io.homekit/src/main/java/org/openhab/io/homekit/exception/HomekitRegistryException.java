package org.openhab.io.homekit.exception;

/**
 * Exception thrown when there is an error during HomeKit registry operations.
 * 
 * This exception is used to indicate problems that occur during registry
 * management operations, such as adding/removing items from registries,
 * registry state inconsistencies, or registry access errors. It extends the
 * base
 * {@link HomekitException} class to provide specific error handling for
 * registry-related issues.
 * 
 * Common scenarios where this exception is thrown:
 * - Registry add/remove operation failures
 * - Registry state inconsistencies
 * - Concurrent modification errors
 * - Registry access permission errors
 * - Registry corruption or data integrity issues
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0
 */
public class HomekitRegistryException extends HomekitException {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new HomeKit registry exception with the specified detail
     * message.
     *
     * @param message the detail message (which is saved for later retrieval by the
     *            {@link #getMessage()} method)
     */
    public HomekitRegistryException(String message) {
        super(message);
    }

    /**
     * Constructs a new HomeKit registry exception with the specified detail message
     * and cause.
     *
     * @param message the detail message (which is saved for later retrieval by the
     *            {@link #getMessage()} method)
     * @param cause the cause (which is saved for later retrieval by the
     *            {@link #getCause()} method)
     */
    public HomekitRegistryException(String message, Throwable cause) {
        super(message, cause);
    }
}
