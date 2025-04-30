package org.openhab.io.homekit.exception;

/**
 * Exception thrown when there is an error during HomeKit service or characteristic registration.
 */
public class HomekitRegistrationException extends HomekitFactoryException {
    public HomekitRegistrationException(String message) {
        super(message);
    }

    public HomekitRegistrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
