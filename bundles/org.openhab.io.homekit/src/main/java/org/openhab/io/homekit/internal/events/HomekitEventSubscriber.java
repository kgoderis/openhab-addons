package org.openhab.io.homekit.internal.events;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public interface HomekitEventSubscriber {
    void onEvent(HomekitEvent event);

    void onEventError(HomekitEvent event, Exception e);
}
