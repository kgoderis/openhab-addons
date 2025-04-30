package org.openhab.io.homekit.exception;

import org.openhab.io.homekit.internal.accessory.AccessoryServerState;

public class HomekitInvalidStateTransitionException extends HomekitServerException {
    public HomekitInvalidStateTransitionException(AccessoryServerState current, AccessoryServerState next) {
        super(String.format("Invalid state transition from %s to %s", current, next));
    }

    public HomekitInvalidStateTransitionException(String message) {
        super(message);
    }
}
