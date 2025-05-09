package org.openhab.io.homekit.api.listener;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.event.model.characteristic.HomekitCharacteristicEvent;

@NonNullByDefault
public interface HomekitCharacteristicChangeListener {
    void onCharacteristicEvent(HomekitCharacteristicEvent event);
}
