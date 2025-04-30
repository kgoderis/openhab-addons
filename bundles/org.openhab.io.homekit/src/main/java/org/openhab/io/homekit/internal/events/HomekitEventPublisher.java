package org.openhab.io.homekit.internal.events;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public interface HomekitEventPublisher {
    String getSourceUID();

    HomekitEventManager getEventManager();

    default void publishEvent(HomekitEvent event) {
        event.setSourceUid(getSourceUID());
        getEventManager().publishEvent(event);
    }
}
