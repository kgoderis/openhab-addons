package org.openhab.io.homekit.exception;

public class AccessoryOperationException extends HomekitServerException {
    public AccessoryOperationException(String message) {
        super(message);
    }

    public AccessoryOperationException(String message, Throwable cause) {
        super(message, cause);
    }
} 