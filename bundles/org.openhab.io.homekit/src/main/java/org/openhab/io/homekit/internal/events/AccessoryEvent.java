package org.openhab.io.homekit.internal.events;

import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.io.homekit.api.hap.Accessory;
import org.openhab.io.homekit.api.hap.Characteristic;
import org.openhab.io.homekit.api.hap.Service;

@NonNullByDefault
public class AccessoryEvent extends AbstractHomekitEvent {

    private final Optional<Accessory> accessory;
    private final Optional<Service> service;
    private final Optional<Characteristic<?>> characteristic;
    private final String oldUID;
    private final String newUID;

    public AccessoryEvent(HomekitEventType type, @Nullable Accessory accessory, @Nullable Service service,
            @Nullable Characteristic<?> characteristic) {
        super(type, accessory != null ? accessory.getUID().toString() : "unknown");
        this.accessory = Optional.ofNullable(accessory);
        this.service = Optional.ofNullable(service);
        this.characteristic = Optional.ofNullable(characteristic);
        this.oldUID = null;
        this.newUID = null;
    }

    public AccessoryEvent(HomekitEventType type, @Nullable Accessory accessory, @Nullable Service service,
            @Nullable Characteristic<?> characteristic, String subscriberUID) {
        super(type, accessory != null ? accessory.getUID().toString() : "unknown", subscriberUID);
        this.accessory = Optional.ofNullable(accessory);
        this.service = Optional.ofNullable(service);
        this.characteristic = Optional.ofNullable(characteristic);
        this.oldUID = null;
        this.newUID = null;
    }

    /**
     * Creates a new AccessoryEvent for UID changes.
     * This constructor is used when an accessory's UID changes, typically when it is assigned to a server.
     *
     * @param type The event type (should be ACCESSORY_UID_CHANGED)
     * @param oldUID The old UID that is being replaced
     * @param newUID The new UID assigned by the server
     */
    public AccessoryEvent(HomekitEventType type, String oldUID, String newUID) {
        super(type, oldUID);
        this.accessory = Optional.empty();
        this.service = Optional.empty();
        this.characteristic = Optional.empty();
        this.oldUID = oldUID;
        this.newUID = newUID;
    }

    public AccessoryEvent(HomekitEventType type, String oldUID, String newUID, String subscriberUID) {
        super(type, oldUID, subscriberUID);
        this.accessory = Optional.empty();
        this.service = Optional.empty();
        this.characteristic = Optional.empty();
        this.oldUID = oldUID;
        this.newUID = newUID;
    }

    public Optional<Accessory> getAccessory() {
        return accessory;
    }

    public Optional<Service> getService() {
        return service;
    }

    public Optional<Characteristic<?>> getCharacteristic() {
        return characteristic;
    }

    /**
     * Gets the old UID that was replaced.
     * @return The old UID
     */
    public String getOldUID() {
        return oldUID;
    }

    /**
     * Gets the new UID that was assigned.
     * @return The new UID
     */
    public String getNewUID() {
        return newUID;
    }

    @Override
    public String toString() {
        return "AccessoryEvent{" + "type=" + getType() + ", publisherUID=" + getPublisherUID() + ", timestamp="
                + getTimestamp() + ", accessory=" + accessory + ", service=" + service + ", characteristic="
                + characteristic + '}';
    }
}
