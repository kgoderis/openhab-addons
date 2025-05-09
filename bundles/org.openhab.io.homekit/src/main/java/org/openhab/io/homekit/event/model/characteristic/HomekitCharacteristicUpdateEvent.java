package org.openhab.io.homekit.event.model.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.UID;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristic;
import org.openhab.io.homekit.api.event.HomekitEventType;
import org.openhab.io.homekit.event.core.HomekitEventMetadata;

/**
 * Represents a characteristic change value event in the Homekit integration.
 * This event is published when a characteristic's value is about to be changed.
 */
@NonNullByDefault
public class HomekitCharacteristicUpdateEvent extends HomekitCharacteristicEvent {

    public HomekitCharacteristicUpdateEvent(UID publisherUID, UID subscriberUID,HomekitCharacteristic<?> characteristic, @Nullable JsonValue oldValue, @Nullable JsonValue newValue,HomekitEventMetadata metadata) {
        super(HomekitEventType.CHARACTERISTIC_CHANGE_VALUE, publisherUID, subscriberUID, characteristic, oldValue, newValue,metadata);
    }

    public HomekitCharacteristicUpdateEvent(HomekitCharacteristic<?> characteristic, @Nullable JsonValue oldValue, @Nullable JsonValue newValue) {
        super(HomekitEventType.CHARACTERISTIC_CHANGE_VALUE, characteristic, oldValue, newValue);
    }

    public HomekitCharacteristicUpdateEvent(HomekitCharacteristic<?> characteristic, @Nullable JsonValue oldValue, @Nullable JsonValue newValue,
            HomekitEventMetadata metadata) {
        super(HomekitEventType.CHARACTERISTIC_CHANGE_VALUE, characteristic, oldValue, newValue, metadata);
    }
}
