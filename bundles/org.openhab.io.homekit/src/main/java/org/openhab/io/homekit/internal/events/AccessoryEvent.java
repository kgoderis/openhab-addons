package org.openhab.io.homekit.internal.events;

import java.util.Collections;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.io.homekit.api.hap.Accessory;
import org.openhab.io.homekit.api.hap.Characteristic;
import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.internal.accessory.AccessoryUID;

/**
 * Represents an accessory-related event in the HomeKit integration.
 * This event is used to propagate changes in accessories, services, and characteristics
 * between different components of the system.
 */
@NonNullByDefault
public class AccessoryEvent extends AbstractHomekitEvent {
    private final Optional< Accessory> accessory;
    private final Optional< Service> service;
    private final Optional< Characteristic<?>> characteristic;
    private final Optional< AccessoryUID> oldUid;
    private final Optional< AccessoryUID> newUid;

    /**
     * Creates a new accessory event with the specified accessory, service, and characteristic.
     *
     * @param accessory the accessory that changed
     * @param service the service that changed, or null if not applicable
     * @param characteristic the characteristic that changed, or null if not applicable
     */
    @SuppressWarnings("null")
    public AccessoryEvent(HomekitEventType type, @Nullable Accessory accessory, @Nullable Service service, @Nullable Characteristic<?> characteristic) {
        super(type, accessory != null ? accessory.getUID() : new HomekitUID("accessory"), HomekitUID.WILDCARD_UID, new EventMetadata(accessory != null ? accessory.getUID() : HomekitUID.WILDCARD_UID, null, null, Collections.emptySet()));
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
    public AccessoryEvent(HomekitEventType type, @Nullable Accessory accessory, @Nullable Service service, @Nullable Characteristic<?> characteristic,
            EventMetadata metadata) {
        super(type, accessory != null ? accessory.getUID() : new HomekitUID("accessory"), HomekitUID.WILDCARD_UID, metadata);
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
    public AccessoryEvent(HomekitEventType type, @Nullable Accessory accessory, AccessoryUID oldUid, AccessoryUID newUid) {
        super(type, accessory != null ? accessory.getUID() : new HomekitUID("accessory"), HomekitUID.WILDCARD_UID, new EventMetadata(accessory != null ? accessory.getUID() : HomekitUID.WILDCARD_UID, null, null, Collections.emptySet()));
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
    public AccessoryEvent(HomekitEventType type, Accessory accessory, AccessoryUID oldUid, AccessoryUID newUid, EventMetadata metadata) {
        super(type, accessory != null ? accessory.getUID() : new HomekitUID("accessory"), HomekitUID.WILDCARD_UID, metadata);
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
    public Optional< Accessory> getAccessory() {
        return accessory;
    }

    /**
     * Returns the service that changed, if applicable.
     *
     * @return the service, or null if not applicable
     */
    public Optional< Service> getService() {
        return service;
    }

    /**
     * Returns the characteristic that changed, if applicable.
     *
     * @return the characteristic, or null if not applicable
     */
    public Optional< Characteristic<?>> getCharacteristic() {
        return characteristic;
    }

    /**
     * Returns the previous UID of the accessory, if this is a UID change event.
     *
     * @return the old UID, or null if not a UID change event
     */
    public Optional< AccessoryUID> getOldUid() {
        return oldUid;
    }

    /**
     * Returns the new UID of the accessory, if this is a UID change event.
     *
     * @return the new UID, or null if not a UID change event
     */
    public Optional< AccessoryUID> getNewUid() {
        return newUid;
    }

    @Override
    public String toString() {
        return String.format("AccessoryEvent{type=%s, accessory=%s, service=%s, characteristic=%s, oldUid=%s, newUid=%s, publisherUID=%s, subscriberUID=%s, timestamp=%d}",
                getType(), accessory, service, characteristic, oldUid, newUid, getPublisherUID(), getSubscriberUID(), getTimestamp());
    }
}
