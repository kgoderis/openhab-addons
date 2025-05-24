package org.openhab.io.homekit.event.model.accessory;

import java.util.Collections;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.UID;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristic;
import org.openhab.io.homekit.api.event.HomekitEventType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.api.uid.HomekitAccessoryUID;
import org.openhab.io.homekit.event.core.AbstractHomekitEvent;
import org.openhab.io.homekit.event.core.HomekitEventMetadata;
import org.openhab.io.homekit.util.HomekitUID;

/**
 * Represents an accessory-related event in the Homekit integration.
 * This event is used to propagate changes in accessories, services, and characteristics
 * between different components of the system.
 */
@NonNullByDefault
public class HomekitAccessoryEvent extends AbstractHomekitEvent {
    private final Optional<HomekitAccessory> accessory;
    private final Optional<HomekitService> service;
    private final Optional<HomekitCharacteristic<?>> characteristic;
    private final Optional<HomekitAccessoryUID> oldUid;
    private final Optional<HomekitAccessoryUID> newUid;

    /**
     * Creates a new accessory event with the specified accessory, service, and characteristic.
     *
     * @param accessory the accessory that changed
     * @param service the service that changed, or null if not applicable
     * @param characteristic the characteristic that changed, or null if not applicable
     */
    @SuppressWarnings("null")
    public HomekitAccessoryEvent(HomekitEventType type, @Nullable HomekitAccessory accessory,
            @Nullable HomekitService service, @Nullable HomekitCharacteristic<?> characteristic) {
        super(type, accessory != null ? (UID) accessory.getUID() : (UID) new HomekitUID("accessory"), HomekitUID.WILDCARD_UID,
                new HomekitEventMetadata(accessory != null ? (UID) accessory.getUID() : (UID) HomekitUID.WILDCARD_UID, null, null,
                        Collections.emptySet()));
        this.accessory = Optional.ofNullable(accessory);
        this.service = Optional.ofNullable(service);
        this.characteristic = Optional.ofNullable(characteristic);
        this.oldUid = Optional.empty();
        this.newUid = Optional.empty();
    }

    /**
     * Creates a new accessory event with the specified accessory, service, characteristic, and metadata.
     *
     * @param accessory the accessory that changed
     * @param service the service that changed, or null if not applicable
     * @param characteristic the characteristic that changed, or null if not applicable
     * @param metadata the event metadata
     */
    @SuppressWarnings("null")
    public HomekitAccessoryEvent(HomekitEventType type, @Nullable HomekitAccessory accessory,
            @Nullable HomekitService service, @Nullable HomekitCharacteristic<?> characteristic,
            HomekitEventMetadata metadata) {
        super(type, accessory != null ? (UID) accessory.getUID() : (UID) new HomekitUID("accessory"), HomekitUID.WILDCARD_UID,
                metadata);
        this.accessory = Optional.ofNullable(accessory);
        this.service = Optional.ofNullable(service);
        this.characteristic = Optional.ofNullable(characteristic);
        this.oldUid = Optional.empty();
        this.newUid = Optional.empty();
    }

    /**
     * Creates a new accessory event for a UID change.
     *
     * @param accessory the accessory that changed
     * @param oldUid the previous UID of the accessory
     * @param newUid the new UID of the accessory
     */
    @SuppressWarnings("null")
    public HomekitAccessoryEvent(HomekitEventType type, @Nullable HomekitAccessory accessory,
            HomekitAccessoryUID oldUid, HomekitAccessoryUID newUid) {
        super(type, accessory != null ? (UID) accessory.getUID() : (UID) new HomekitUID("accessory"), HomekitUID.WILDCARD_UID,
                new HomekitEventMetadata(accessory != null ? (UID) accessory.getUID() : (UID) HomekitUID.WILDCARD_UID, null, null,
                        Collections.emptySet()));
        this.accessory = Optional.ofNullable(accessory);
        this.service = Optional.empty();
        this.characteristic = Optional.empty();
        this.oldUid = Optional.of(oldUid);
        this.newUid = Optional.of(newUid);
    }

    /**
     * Creates a new accessory event for a UID change with metadata.
     *
     * @param accessory the accessory that changed
     * @param oldUid the previous UID of the accessory
     * @param newUid the new UID of the accessory
     * @param metadata the event metadata
     */
    @SuppressWarnings("null")
    public HomekitAccessoryEvent(HomekitEventType type, HomekitAccessory accessory, HomekitAccessoryUID oldUid,
            HomekitAccessoryUID newUid, HomekitEventMetadata metadata) {
        super(type, accessory != null ? (UID) accessory.getUID() : (UID) new HomekitUID("accessory"), HomekitUID.WILDCARD_UID,
                metadata);
        this.accessory = Optional.ofNullable(accessory);
        this.service = Optional.empty();
        this.characteristic = Optional.empty();
        this.oldUid = Optional.of(oldUid);
        this.newUid = Optional.of(newUid);
    }

    /**
     * Returns the accessory that changed.
     *
     * @return the accessory
     */
    public Optional<HomekitAccessory> getAccessory() {
        return accessory;
    }

    /**
     * Returns the service that changed, if applicable.
     *
     * @return the service, or null if not applicable
     */
    public Optional<HomekitService> getService() {
        return service;
    }

    /**
     * Returns the characteristic that changed, if applicable.
     *
     * @return the characteristic, or null if not applicable
     */
    public Optional<HomekitCharacteristic<?>> getCharacteristic() {
        return characteristic;
    }

    /**
     * Returns the previous UID of the accessory, if this is a UID change event.
     *
     * @return the old UID, or null if not a UID change event
     */
    public Optional<HomekitAccessoryUID> getOldUid() {
        return oldUid;
    }

    /**
     * Returns the new UID of the accessory, if this is a UID change event.
     *
     * @return the new UID, or null if not a UID change event
     */
    public Optional<HomekitAccessoryUID> getNewUid() {
        return newUid;
    }

    @Override
    public String toString() {
        return String.format(
                "HomekitAccessoryEvent{type=%s, accessory=%s, service=%s, characteristic=%s, oldUid=%s, newUid=%s, publisherUID=%s, subscriberUID=%s, timestamp=%d}",
                getType(), accessory, service, characteristic, oldUid, newUid, getPublisherUID(), getSubscriberUID(),
                getTimestamp());
    }
}
