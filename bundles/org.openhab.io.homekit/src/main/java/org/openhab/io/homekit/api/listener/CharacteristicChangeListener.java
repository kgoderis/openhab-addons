package org.openhab.io.homekit.api.listener;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.internal.events.CharacteristicEvent;

@NonNullByDefault
public interface CharacteristicChangeListener {
    void onCharacteristicEvent(CharacteristicEvent event);
}
