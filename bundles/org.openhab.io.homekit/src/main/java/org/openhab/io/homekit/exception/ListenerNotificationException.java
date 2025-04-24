package org.openhab.io.homekit.exception;

public class ListenerNotificationException extends HomekitServerException {
    public ListenerNotificationException(String message) {
        super(message);
    }

    public ListenerNotificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
