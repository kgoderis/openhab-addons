package org.openhab.io.homekit.internal.factory;

/**
 * Exception thrown when there is an error with HomeKit metadata.
 */
public class MetadataException extends HomekitFactoryException {
    public MetadataException(String message) {
        super(message);
    }

    public MetadataException(String message, Throwable cause) {
        super(message, cause);
    }
}
