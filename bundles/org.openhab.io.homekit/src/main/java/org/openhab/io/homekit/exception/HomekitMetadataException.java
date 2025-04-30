package org.openhab.io.homekit.exception;

/**
 * Exception thrown when there is an error with HomeKit metadata.
 */
public class HomekitMetadataException extends HomekitFactoryException {
    public HomekitMetadataException(String message) {
        super(message);
    }

    public HomekitMetadataException(String message, Throwable cause) {
        super(message, cause);
    }
}
