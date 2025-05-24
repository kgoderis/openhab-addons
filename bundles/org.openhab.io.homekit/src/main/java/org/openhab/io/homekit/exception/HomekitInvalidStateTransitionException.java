package org.openhab.io.homekit.exception;

import org.openhab.io.homekit.core.server.HomekitAccessoryServerState;

public class HomekitInvalidStateTransitionException extends HomekitServerException {
    public HomekitInvalidStateTransitionException(HomekitAccessoryServerState current,
            HomekitAccessoryServerState next) {
        super(String.format("Invalid state transition from %s to %s", current, next));
    }

    public HomekitInvalidStateTransitionException(String message) {
        super(message);
    }
}
