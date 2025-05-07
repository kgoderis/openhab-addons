package org.openhab.io.homekit.internal.events;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.UID;
import org.openhab.io.homekit.api.hap.Characteristic;

/**
 * Represents a characteristic change value event in the HomeKit integration.
 * This event is published when a characteristic's value is about to be changed.
 */
@NonNullByDefault
public class CharacteristicChangeValueEvent extends CharacteristicEvent {

    public CharacteristicChangeValueEvent(UID publisherUID, UID subscriberUID,Characteristic<?> characteristic, @Nullable JsonValue oldValue, @Nullable JsonValue newValue,EventMetadata metadata) {
        super(HomekitEventType.CHARACTERISTIC_CHANGE_VALUE, publisherUID, subscriberUID, characteristic, oldValue, newValue,metadata);
    }

    public CharacteristicChangeValueEvent(Characteristic<?> characteristic, @Nullable JsonValue oldValue, @Nullable JsonValue newValue) {
        super(HomekitEventType.CHARACTERISTIC_CHANGE_VALUE, characteristic, oldValue, newValue);
    }

    public CharacteristicChangeValueEvent(Characteristic<?> characteristic, @Nullable JsonValue oldValue, @Nullable JsonValue newValue,
            EventMetadata metadata) {
        super(HomekitEventType.CHARACTERISTIC_CHANGE_VALUE, characteristic, oldValue, newValue, metadata);
    }
} 