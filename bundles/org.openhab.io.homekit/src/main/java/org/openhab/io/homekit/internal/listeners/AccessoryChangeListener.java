package org.openhab.io.homekit.internal.listeners;

import org.openhab.io.homekit.internal.events.AccessoryEvent;

public interface AccessoryChangeListener {
    void onAccessoryEvent(AccessoryEvent event);
}