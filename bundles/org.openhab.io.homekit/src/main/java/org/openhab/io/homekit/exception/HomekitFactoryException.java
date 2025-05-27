package org.openhab.io.homekit.exception;

/**
 * Exception thrown when there is an error during HomeKit factory operations.
 * 
 * This exception is used to indicate problems that occur during the creation
 * or initialization of HomeKit components through factory methods, such as
 * accessory creation, service instantiation, or characteristic factory operations.
 * It extends the base {@link HomekitException} class to provide specific error
 * handling for factory-related issues.
 * 
 * Common scenarios where this exception is thrown:
 * - Accessory creation failures
 * - Service instantiation errors
 * - Characteristic factory failures
 * - Invalid factory parameters
 * - Resource allocation errors
 * - Factory initialization failures
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0
 */
public class HomekitFactoryException extends HomekitException {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new HomeKit factory exception with the specified detail message.
     *
     * @param message the detail message (which is saved for later retrieval by the
     *            {@link #getMessage()} method)
     */
    public HomekitFactoryException(String message) {
        super(message);
    }

    /**
     * Constructs a new HomeKit factory exception with the specified detail message and cause.
     *
     * @param message the detail message (which is saved for later retrieval by the
     *            {@link #getMessage()} method)
     * @param cause the cause (which is saved for later retrieval by the
     *            {@link #getCause()} method)
     */
    public HomekitFactoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
