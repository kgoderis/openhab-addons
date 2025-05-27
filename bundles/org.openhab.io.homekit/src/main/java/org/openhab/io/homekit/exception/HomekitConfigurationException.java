package org.openhab.io.homekit.exception;

/**
 * Exception thrown when there is an error with HomeKit configuration.
 * 
 * This exception is used to indicate problems that occur during the configuration
 * of HomeKit components, such as invalid configuration parameters, missing required
 * settings, or configuration validation failures. It extends {@link HomekitServerException}
 * to provide specific error handling for configuration-related issues.
 * 
 * Common scenarios where this exception is thrown:
 * - Invalid configuration parameters
 * - Missing required settings
 * - Configuration validation failures
 * - Configuration file parsing errors
 * - Configuration update failures
 * - Incompatible configuration versions
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0
 */
public class HomekitConfigurationException extends HomekitServerException {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new HomeKit configuration exception with the specified detail message.
     *
     * @param message the detail message (which is saved for later retrieval by the
     *            {@link #getMessage()} method)
     */
    public HomekitConfigurationException(String message) {
        super(message);
    }

    /**
     * Constructs a new HomeKit configuration exception with the specified detail message and cause.
     *
     * @param message the detail message (which is saved for later retrieval by the
     *            {@link #getMessage()} method)
     * @param cause the cause (which is saved for later retrieval by the
     *            {@link #getCause()} method)
     */
    public HomekitConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new HomeKit configuration exception with the specified cause.
     *
     * @param cause the cause (which is saved for later retrieval by the
     *            {@link #getCause()} method)
     */
    public HomekitConfigurationException(Throwable cause) {
        super(cause);
    }
}
