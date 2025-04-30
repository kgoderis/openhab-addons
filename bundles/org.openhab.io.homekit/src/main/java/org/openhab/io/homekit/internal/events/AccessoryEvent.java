package org.openhab.io.homekit.internal.events;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.io.homekit.api.hap.Accessory;
import org.openhab.io.homekit.api.hap.Service;

public class AccessoryEvent extends AbstractHomekitEvent {

    private final Accessory accessory;
    private final Service service;

    public AccessoryEvent(@NonNull Accessory accessory, @NonNull Service service,
            @NonNull HomekitEventType accessoryEventType) {
        // Use the accessory UID as the sourceUid, and map the event type to a HomekitEventType
        super(accessory.getUID().toString(), accessoryEventType);
        this.accessory = accessory;
        this.service = service;
    }

    public Accessory getAccessory() {
        return accessory;
    }

    public Service getService() {
        return service;
    }

    @Override
    public String toString() {
        return "AccessoryEvent{" + "accessory=" + accessory + ", service=" + service + ", sourceUid=" + getSourceUid()
                + ", timestamp=" + getTimestamp() + ", type=" + getType() + '}';
    }
}
