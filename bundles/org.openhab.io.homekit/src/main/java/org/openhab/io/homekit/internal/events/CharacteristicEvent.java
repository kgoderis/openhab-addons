package org.openhab.io.homekit.internal.events;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.hap.Characteristic;

@NonNullByDefault
public class CharacteristicEvent extends AbstractHomekitEvent {
    private final Characteristic<?> characteristic;
    private final JsonValue oldValue;
    private final JsonValue newValue;

    public CharacteristicEvent(HomekitEventType type, Characteristic<?> characteristic, JsonValue oldValue,
            JsonValue newValue) {
        super(characteristic.getUID().toString(), type);
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

    @Override
    public String toString() {
        return "CharacteristicEvent[type=" + getType() + ", characteristic=" + characteristic + ", oldValue=" + oldValue
                + ", newValue=" + newValue + "]";
    }
}
