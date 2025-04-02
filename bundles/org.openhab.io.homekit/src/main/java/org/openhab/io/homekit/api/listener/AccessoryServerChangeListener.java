package org.openhab.io.homekit.api.listener;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.internal.events.AccessoryServerEvent;

@NonNullByDefault
public interface AccessoryServerChangeListener {
    void onAccessoryServerEvent(AccessoryServerEvent event);
}
