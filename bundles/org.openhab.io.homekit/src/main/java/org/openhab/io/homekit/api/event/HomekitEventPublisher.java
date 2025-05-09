package org.openhab.io.homekit.api.event;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.UID;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

@NonNullByDefault
public interface HomekitEventPublisher {
    UID getSourceUID();

    HomekitEventManager getEventManager();

    default void publishEvent(HomekitEvent event) {
        event.setPublisherUID(getSourceUID());
        getEventManager().publishEvent(event);
    }
}
