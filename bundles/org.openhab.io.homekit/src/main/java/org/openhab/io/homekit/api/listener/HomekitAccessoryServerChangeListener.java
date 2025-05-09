package org.openhab.io.homekit.api.listener;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.event.model.server.HomekitAccessoryServerEvent;
@NonNullByDefault
public interface HomekitAccessoryServerChangeListener {
    void onAccessoryServerEvent(HomekitAccessoryServerEvent event);
}
