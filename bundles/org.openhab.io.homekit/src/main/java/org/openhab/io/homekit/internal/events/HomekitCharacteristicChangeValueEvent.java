package org.openhab.io.homekit.internal.events;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.UID;
import org.openhab.io.homekit.api.hap.HomekitCharacteristic;

/**
 * Represents a characteristic change value event in the Homekit integration.
 * This event is published when a characteristic's value is about to be changed.
 */
@NonNullByDefault
public class HomekitCharacteristicChangeValueEvent extends HomekitCharacteristicEvent {

    public HomekitCharacteristicChangeValueEvent(UID publisherUID, UID subscriberUID,HomekitCharacteristic<?> characteristic, @Nullable JsonValue oldValue, @Nullable JsonValue newValue,HomekitEventMetadata metadata) {
        super(HomekitEventType.CHARACTERISTIC_CHANGE_VALUE, publisherUID, subscriberUID, characteristic, oldValue, newValue,metadata);
    }

    public HomekitCharacteristicChangeValueEvent(HomekitCharacteristic<?> characteristic, @Nullable JsonValue oldValue, @Nullable JsonValue newValue) {
        super(HomekitEventType.CHARACTERISTIC_CHANGE_VALUE, characteristic, oldValue, newValue);
    }

    public HomekitCharacteristicChangeValueEvent(HomekitCharacteristic<?> characteristic, @Nullable JsonValue oldValue, @Nullable JsonValue newValue,
            HomekitEventMetadata metadata) {
        super(HomekitEventType.CHARACTERISTIC_CHANGE_VALUE, characteristic, oldValue, newValue, metadata);
    }
}
