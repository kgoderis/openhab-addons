package org.openhab.io.homekit.internal.events;

import java.util.Collections;
import java.util.Optional;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.UID;
import org.openhab.io.homekit.api.hap.HomekitCharacteristic;

/**
 * Represents a characteristic value change event in the HomeKit integration.
 * This event is used to propagate changes in characteristic values between
 * different components of the system.
 */
@NonNullByDefault
public class HomekitCharacteristicEvent extends AbstractHomekitEvent {
    private final Optional<HomekitCharacteristic<?>> characteristic;
    private final Optional<JsonValue> oldValue;
    private final Optional<JsonValue> newValue;

    /**
     * Creates a new characteristic event with the specified characteristic and values.
     *
     * @param characteristic the characteristic that changed
     * @param oldValue the previous value of the characteristic
     * @param newValue the new value of the characteristic
     */
    @SuppressWarnings("null")
    public HomekitCharacteristicEvent(HomekitEventType type, UID publisherUID, UID subscriberUID,HomekitCharacteristic<?> characteristic, @Nullable JsonValue oldValue, @Nullable JsonValue newValue,HomekitEventMetadata metadata) {
        super(type,  publisherUID,  subscriberUID, metadata);
        this.characteristic = Optional.ofNullable(characteristic);
        this.oldValue = Optional.ofNullable(oldValue);
        this.newValue = Optional.ofNullable(newValue);
    }

    /**
     * Creates a new characteristic event with the specified characteristic and values.
     *
     * @param characteristic the characteristic that changed
     * @param oldValue the previous value of the characteristic
     * @param newValue the new value of the characteristic
     */
    @SuppressWarnings("null")
    public HomekitCharacteristicEvent(HomekitEventType type, HomekitCharacteristic<?> characteristic, @Nullable JsonValue oldValue, @Nullable JsonValue newValue) {
        super(type, characteristic != null ? characteristic.getUID() : new HomekitUID("characteristic"), HomekitUID.WILDCARD_UID, new HomekitEventMetadata(characteristic != null ? characteristic.getUID() : new HomekitUID("characteristic"), null, null, Collections.emptySet()));
        this.characteristic = Optional.ofNullable(characteristic);
        this.oldValue = Optional.ofNullable(oldValue);
        this.newValue = Optional.ofNullable(newValue);
    }

    /**
     * Creates a new characteristic event with the specified characteristic, values, and metadata.
     *
     * @param characteristic the characteristic that changed
     * @param oldValue the previous value of the characteristic
     * @param newValue the new value of the characteristic
     * @param metadata the event metadata
     */
    @SuppressWarnings("null")
    public HomekitCharacteristicEvent(HomekitEventType type, HomekitCharacteristic<?> characteristic, @Nullable JsonValue oldValue, @Nullable JsonValue newValue,
            HomekitEventMetadata metadata) {
        super(type, characteristic != null ? characteristic.getUID() : new HomekitUID("characteristic"), HomekitUID.WILDCARD_UID, metadata);
        this.characteristic = Optional.ofNullable(characteristic);
        this.oldValue = Optional.ofNullable(oldValue);
        this.newValue = Optional.ofNullable(newValue);
    }

    /**
     * Returns the characteristic that changed.
     *
     * @return the characteristic
     */
    public Optional<HomekitCharacteristic<?>> getCharacteristic() {
        return characteristic;
    }

    public Optional<JsonValue> getOldValue() {
        return oldValue;
    }

    public Optional<JsonValue> getNewValue() {
        return newValue;
    }

    @Override
    public String toString() {
        return String.format(
                "HomekitCharacteristicEvent{type=%s, characteristic=%s, oldValue=%s, newValue=%s, publisherUID=%s, subscriberUID=%s, timestamp=%d}",
                getType(), characteristic, oldValue, newValue, getPublisherUID(), getSubscriberUID(), getTimestamp());
    }
}
