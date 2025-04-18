package org.openhab.io.homekit.internal.events;

import javax.json.JsonValue;

import org.openhab.io.homekit.api.hap.Characteristic;

public class CharacteristicEvent {
    private final Characteristic<?> characteristic;
    private final JsonValue oldValue;
    private final JsonValue newValue;
    private final CharacteristicEventType eventType;

    public enum CharacteristicEventType {
        CHARACTERISTIC_STATE_CHANGED,
        CHARACTERISTIC_START_EVENTS,
        CHARACTERISTIC_STOP_EVENTS
    }

    public CharacteristicEvent(Characteristic<?> characteristic, CharacteristicEventType eventType) {
        this(characteristic, null, null, eventType);
    }

    public CharacteristicEvent(Characteristic<?> characteristic, JsonValue oldValue, JsonValue newValue) {
        this(characteristic, oldValue, newValue, CharacteristicEventType.CHARACTERISTIC_STATE_CHANGED);
    }

    public CharacteristicEvent(Characteristic<?> characteristic, JsonValue oldValue, JsonValue newValue,
            CharacteristicEventType eventType) {
        this.characteristic = characteristic;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.eventType = eventType;
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

    public CharacteristicEventType getEventType() {
        return eventType;
    }
}
