package org.openhab.io.homekit.internal.events;

import javax.json.JsonValue;

import org.openhab.io.homekit.api.Characteristic;


public class CharacteristicEvent {
    private final Characteristic<?> characteristic;
    private final JsonValue oldValue;
    private final JsonValue newValue;

    public CharacteristicEvent(Characteristic<?> characteristic, JsonValue oldValue, JsonValue newValue) {
        this.characteristic = characteristic;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public Characteristic<?> getCharacteristic() {
        return characteristic;
    }

    public JsonValue getOldValue() {
        return oldValue;
    }

    public JsonValue getNewValue() {
        return newValue;
    }
}