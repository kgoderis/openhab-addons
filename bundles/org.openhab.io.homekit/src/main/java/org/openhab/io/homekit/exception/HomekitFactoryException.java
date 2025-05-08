package org.openhab.io.homekit.exception;

/**
 * Base exception class for Homekit factory related errors.
 */
public class HomekitFactoryException extends HomekitException {
    public HomekitFactoryException(String message) {
        super(message);
    }

    public HomekitFactoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
