package org.openhab.io.homekit.internal.events;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.UID;

@NonNullByDefault
public interface HomekitEventPublisher {
    UID getSourceUID();

    HomekitEventManager getEventManager();

    default void publishEvent(HomekitEvent event) {
        event.setPublisherUID(getSourceUID());
        getEventManager().publishEvent(event);
    }
}
