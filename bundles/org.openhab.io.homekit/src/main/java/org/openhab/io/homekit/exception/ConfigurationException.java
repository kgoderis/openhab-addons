package org.openhab.io.homekit.exception;

/**
 * Exception thrown when there is an error with HomeKit server configuration.
 */
public class ConfigurationException extends HomekitServerException {
    private static final long serialVersionUID = 1L;

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConfigurationException(Throwable cause) {
        super(cause);
    }
}
