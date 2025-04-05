package org.openhab.io.homekit.internal.events;

import org.openhab.io.homekit.api.hap.Accessory;
import org.openhab.io.homekit.api.hap.Characteristic;
import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.api.server.AccessoryServer;

public class AccessoryServerEvent {
    private final AccessoryServer server;
    private final Accessory accessory;
    private final Service service;
    private final Characteristic<?> characteristic;
    private final AccessoryServerEventType type;

    public enum AccessoryServerEventType {
        SERVER_UPDATED,
        ACCESSORY_ADDED,
        ACCESSORY_REMOVED,
        SERVICE_ADDED,
        SERVICE_REMOVED,
        CHARACTERISTIC_ADDED,
        CHARACTERISTIC_REMOVED,
        CHARACTERISTIC_STATE_CHANGED, SERVER_STATE_DISCONNECTED, SERVER_STATE_CONNECTED, SERVER_STATE_PAIRED, SERVER_STATE_PAIR_VERIFIED
    }

    public AccessoryServerEvent(AccessoryServer server, Accessory accessory, Service service, Characteristic<?> characteristic, AccessoryServerEventType type) {
        this.server = server;
        this.accessory = accessory;
        this.service = service;
        this.characteristic = characteristic;
        this.type = type;
    }

    public AccessoryServer getServer() {
        return server;
    }

    public Accessory getAccessory() {
        return accessory;
    }

    public Service getService() {
        return service;
    }

    public Characteristic<?> getCharacteristic() {
        return characteristic;
    }

    public AccessoryServerEventType getType() {
        return type;
    }
} 