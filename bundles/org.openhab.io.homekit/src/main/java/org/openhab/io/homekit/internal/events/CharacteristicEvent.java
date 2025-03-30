package org.openhab.io.homekit.internal.events;

import org.openhab.io.homekit.api.Characteristic;

public class CharacteristicEvent {
    private final Characteristic characteristic;
    private final Object oldValue;
    private final Object newValue;

    public CharacteristicEvent(Characteristic characteristic, Object oldValue, Object newValue) {
        this.characteristic = characteristic;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public Characteristic getCharacteristic() {
        return characteristic;
    }

    public Object getOldValue() {
        return oldValue;
    }

    public Object getNewValue() {
        return newValue;
    }
}