package org.openhab.io.homekit.api.listener;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.internal.events.HomekitCharacteristicEvent;

@NonNullByDefault
public interface HomekitCharacteristicChangeListener {
    void onCharacteristicEvent(HomekitCharacteristicEvent event);
}
