package org.openhab.io.homekit.internal.events;

import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.io.homekit.api.hap.Accessory;
import org.openhab.io.homekit.api.hap.AccessoryServer;
import org.openhab.io.homekit.api.hap.Characteristic;
import org.openhab.io.homekit.api.hap.Service;

@NonNullByDefault
public class AccessoryServerEvent extends AbstractHomekitEvent {
    private final AccessoryServer server;
    private final Optional<Accessory> accessory;
    private final Optional<Service> service;
    private final Optional<Characteristic<?>> characteristic;

    public AccessoryServerEvent(HomekitEventType type, AccessoryServer server, @Nullable Accessory accessory,
            @Nullable Service service, @Nullable Characteristic<?> characteristic) {
        super(type, server.getUID().toString());
        this.server = server;
        this.accessory = Optional.ofNullable(accessory);
        this.service = Optional.ofNullable(service);
        this.characteristic = Optional.ofNullable(characteristic);
    }

    public AccessoryServer getServer() {
        return server;
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

    @Override
    public String toString() {
        return "AccessoryServerEvent{" + "type=" + getType() + ", publisherUID=" + getPublisherUID() + ", timestamp="
                + getTimestamp() + ", server=" + server + ", accessory=" + accessory + ", service=" + service
                + ", characteristic=" + characteristic + '}';
    }
}
