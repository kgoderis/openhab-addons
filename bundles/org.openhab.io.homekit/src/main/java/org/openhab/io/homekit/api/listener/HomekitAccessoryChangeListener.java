package org.openhab.io.homekit.api.listener;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.internal.events.HomekitAccessoryEvent;

@NonNullByDefault
public interface HomekitAccessoryChangeListener {
    void onAccessoryEvent(HomekitAccessoryEvent accessoryEvent);
}
