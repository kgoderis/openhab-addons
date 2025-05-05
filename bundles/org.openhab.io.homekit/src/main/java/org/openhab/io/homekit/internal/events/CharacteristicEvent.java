package org.openhab.io.homekit.internal.events;

import java.util.Optional;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.io.homekit.api.hap.Characteristic;

@NonNullByDefault
public class CharacteristicEvent extends AbstractHomekitEvent {
    private final Optional<Characteristic<?>> characteristic;
    private final JsonValue oldValue;
    private final JsonValue newValue;

    public CharacteristicEvent(HomekitEventType type, @Nullable Characteristic<?> characteristic, JsonValue oldValue,
            JsonValue newValue) {
        super(type, characteristic != null ? characteristic.getUID().toString() : "unknown");
        this.characteristic = Optional.ofNullable(characteristic);
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public CharacteristicEvent(HomekitEventType type, @Nullable Characteristic<?> characteristic, JsonValue oldValue,
            JsonValue newValue, String subscriberUID) {
        super(type, characteristic != null ? characteristic.getUID().toString() : "unknown", subscriberUID);
        this.characteristic = Optional.ofNullable(characteristic);
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public Optional<Characteristic<?>> getCharacteristic() {
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
        return "CharacteristicEvent{" + "type=" + getType() + ", publisherUID=" + getPublisherUID() + ", timestamp="
                + getTimestamp() + ", characteristic=" + characteristic + ", oldValue=" + oldValue + ", newValue="
                + newValue + '}';
    }
}
