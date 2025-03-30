package org.openhab.io.homekit.internal.events;

import org.openhab.io.homekit.api.Characteristic;
import org.openhab.io.homekit.api.Service;

public class ServiceEvent {
    private final Service service;
    private final Characteristic characteristic;
    private final ServiceEventType type;

    public enum ServiceEventType {
        CHARACTERISTIC_ADDED,
        CHARACTERISTIC_REMOVED,
        CHARACTERISTIC_STATE_CHANGED
    }

    public ServiceEvent(Service service, Characteristic characteristic, ServiceEventType type) {
        this.service = service;
        this.characteristic = characteristic;
        this.type = type;
    }

    public Service getService() {
        return service;
    }

    public Characteristic getCharacteristic() {
        return characteristic;
    }

    public ServiceEventType getType() {
        return type;
    }
} 