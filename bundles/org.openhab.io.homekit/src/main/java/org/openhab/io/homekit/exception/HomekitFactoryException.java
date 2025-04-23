package org.openhab.io.homekit.internal.factory;

/**
 * Base exception class for HomeKit factory related errors.
 */
public class HomekitFactoryException extends Exception {
    public HomekitFactoryException(String message) {
        super(message);
    }

    public HomekitFactoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
