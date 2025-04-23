package org.openhab.io.homekit.internal.factory;

/**
 * Exception thrown when there is an error during HomeKit service or characteristic registration.
 */
public class RegistrationException extends HomekitFactoryException {
    public RegistrationException(String message) {
        super(message);
    }

    public RegistrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
