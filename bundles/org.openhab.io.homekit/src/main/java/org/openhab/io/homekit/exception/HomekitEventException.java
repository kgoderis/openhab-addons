package org.openhab.io.homekit.exception;

public class HomekitEventException extends HomekitServerException {
    public HomekitEventException(String message) {
        super(message);
    }

    public HomekitEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
