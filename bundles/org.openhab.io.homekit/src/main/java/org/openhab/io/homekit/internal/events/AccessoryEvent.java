package org.openhab.io.homekit.internal.events;

import org.openhab.io.homekit.api.hap.Accessory;
import org.openhab.io.homekit.api.hap.Service;

public class AccessoryEvent {
    private final Accessory accessory;
    private final Service service;
    private final AccessoryEventType type;

    public enum AccessoryEventType {
        SERVICE_ADDED,
        SERVICE_REMOVED,
        SERVICE_STATE_CHANGED
    }

    public AccessoryEvent(Accessory accessory, Service service, AccessoryEventType type) {
        this.accessory = accessory;
        this.service = service;
        this.type = type;
    }

    public Accessory getAccessory() {
        return accessory;
    }

    public Service getService() {
        return service;
    }

    public AccessoryEventType getType() {
        return type;
    }
} 