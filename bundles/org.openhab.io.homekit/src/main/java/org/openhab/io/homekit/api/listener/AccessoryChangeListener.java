package org.openhab.io.homekit.api.listener;

import org.openhab.io.homekit.internal.events.AccessoryEvent;

public interface AccessoryChangeListener {
    void onAccessoryEvent(AccessoryEvent event);
}