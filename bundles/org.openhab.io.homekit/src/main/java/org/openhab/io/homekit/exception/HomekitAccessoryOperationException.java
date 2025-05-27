package org.openhab.io.homekit.exception;

/**
 * Exception thrown when there is an error during HomeKit accessory operations.
 * 
 * This exception is used to indicate problems that occur during operations on
 * HomeKit accessories, such as reading or writing characteristic values,
 * or managing accessory state. It extends {@link HomekitServerException} to
 * provide specific error handling for accessory-related operations.
 * 
 * Common scenarios where this exception is thrown:
 * - Characteristic value read/write failures
 * - Invalid accessory state transitions
 * - Accessory initialization errors
 * - Service operation failures
 * - Characteristic validation errors
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0
 */
public class HomekitAccessoryOperationException extends HomekitServerException {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new HomeKit accessory operation exception with the specified detail message.
     *
     * @param message the detail message (which is saved for later retrieval by the
     *            {@link #getMessage()} method)
     */
    public HomekitAccessoryOperationException(String message) {
        super(message);
    }

    /**
     * Constructs a new HomeKit accessory operation exception with the specified detail message and cause.
     *
     * @param message the detail message (which is saved for later retrieval by the
     *            {@link #getMessage()} method)
     * @param cause the cause (which is saved for later retrieval by the
     *            {@link #getCause()} method)
     */
    public HomekitAccessoryOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
