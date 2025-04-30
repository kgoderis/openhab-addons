package org.openhab.io.homekit.exception;

public class HomekitAccessoryOperationException extends HomekitServerException {
    public HomekitAccessoryOperationException(String message) {
        super(message);
    }

    public HomekitAccessoryOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
