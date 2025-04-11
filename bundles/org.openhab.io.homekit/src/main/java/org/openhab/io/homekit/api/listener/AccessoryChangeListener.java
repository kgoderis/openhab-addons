package org.openhab.io.homekit.api.listener;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.internal.events.AccessoryEvent;

@NonNullByDefault
public interface AccessoryChangeListener {
    void onAccessoryEvent(AccessoryEvent accessoryEvent);
}
