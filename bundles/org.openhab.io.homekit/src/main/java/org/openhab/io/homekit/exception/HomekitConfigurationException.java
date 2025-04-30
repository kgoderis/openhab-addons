package org.openhab.io.homekit.exception;

/**
 * Exception thrown when there is an error with HomeKit server configuration.
 */
public class HomekitConfigurationException extends HomekitServerException {
    private static final long serialVersionUID = 1L;

    public HomekitConfigurationException(String message) {
        super(message);
    }

    public HomekitConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    public HomekitConfigurationException(Throwable cause) {
        super(cause);
    }
}
