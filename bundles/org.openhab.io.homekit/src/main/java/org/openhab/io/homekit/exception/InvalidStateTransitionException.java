package org.openhab.io.homekit.exception;

import org.openhab.io.homekit.internal.accessory.AccessoryServerState;

public class InvalidStateTransitionException extends HomekitServerException {
    public InvalidStateTransitionException(AccessoryServerState current, AccessoryServerState next) {
        super(String.format("Invalid state transition from %s to %s", current, next));
    }

    public InvalidStateTransitionException(String message) {
        super(message);
    }
}
