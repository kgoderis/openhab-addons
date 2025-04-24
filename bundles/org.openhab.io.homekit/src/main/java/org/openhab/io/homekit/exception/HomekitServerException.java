package org.openhab.io.homekit.exception;

/**
 * Exception thrown when there is an error with HomeKit server operations.
 */
public class HomekitServerException extends HomekitException {
    private static final long serialVersionUID = 1L;

    public HomekitServerException(String message) {
        super(message);
    }

    public HomekitServerException(String message, Throwable cause) {
        super(message, cause);
    }

    public HomekitServerException(Throwable cause) {
        super(cause);
    }
}
