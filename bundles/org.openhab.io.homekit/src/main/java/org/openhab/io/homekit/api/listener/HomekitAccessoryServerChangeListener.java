package org.openhab.io.homekit.api.listener;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.internal.events.HomekitAccessoryServerEvent;

@NonNullByDefault
public interface HomekitAccessoryServerChangeListener {
    void onAccessoryServerEvent(HomekitAccessoryServerEvent event);
}
