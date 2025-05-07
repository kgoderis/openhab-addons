package org.openhab.io.homekit.internal.events;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.io.homekit.api.hap.Characteristic;

/**
 * Represents a characteristic value changed event in the HomeKit integration.
 * This event is published when a characteristic's value has been changed.
 */
@NonNullByDefault
public class CharacteristicValueChangedEvent extends CharacteristicEvent {

    public CharacteristicValueChangedEvent(Characteristic<?> characteristic, @Nullable JsonValue oldValue, @Nullable JsonValue newValue) {
        super(HomekitEventType.CHARACTERISTIC_VALUE_CHANGED, characteristic, oldValue, newValue);
    }

    public CharacteristicValueChangedEvent(Characteristic<?> characteristic, @Nullable JsonValue oldValue, @Nullable JsonValue newValue,
            EventMetadata metadata) {
        super(HomekitEventType.CHARACTERISTIC_VALUE_CHANGED, characteristic, oldValue, newValue, metadata);
    }
} 