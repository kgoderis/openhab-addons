package org.openhab.io.homekit.api.event;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public interface HomekitEventSubscriber {
    void onEvent(HomekitEvent event);

    void onEventError(HomekitEvent event, Exception e);
}
