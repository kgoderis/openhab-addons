package org.openhab.io.homekit/internal/listeners;

import org.openhab.io.homekit.internal.events.CharacteristicEvent;

public interface CharacteristicChangeListener {
    void onCharacteristicEvent(CharacteristicEvent event);
} 