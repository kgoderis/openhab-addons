package org.openhab.io.homekit.event.model.server;

import java.util.Collections;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristic;
import org.openhab.io.homekit.api.server.HomekitAccessoryServer;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.event.core.AbstractHomekitEvent;
import org.openhab.io.homekit.event.core.HomekitEventMetadata;
import org.openhab.io.homekit.api.event.HomekitEventType;
import org.openhab.io.homekit.util.HomekitUID;

@NonNullByDefault
public class HomekitAccessoryServerEvent extends AbstractHomekitEvent {
    private final Optional<HomekitAccessoryServer> server;
    private final Optional<HomekitAccessory> accessory;
    private final Optional<HomekitService> service;
    private final Optional<HomekitCharacteristic<?>> characteristic;

    @SuppressWarnings("null")
    public HomekitAccessoryServerEvent(HomekitEventType type, HomekitAccessoryServer server, @Nullable HomekitAccessory accessory,
            @Nullable HomekitService service, @Nullable HomekitCharacteristic<?> characteristic) {
        super(type, server != null ? server.getUID() : new HomekitUID("server"), WILDCARD_UID, new HomekitEventMetadata(server != null ? server.getUID() : new HomekitUID("server"), null, null, Collections.emptySet()));
        this.server = Optional.ofNullable(server);
        this.accessory = Optional.ofNullable(accessory);
        this.service = Optional.ofNullable(service);
        this.characteristic = Optional.ofNullable(characteristic);
    }

    @SuppressWarnings("null")
    public HomekitAccessoryServerEvent(HomekitEventType type, HomekitAccessoryServer server, @Nullable HomekitAccessory accessory,
    @Nullable HomekitService service, @Nullable HomekitCharacteristic<?> characteristic, HomekitEventMetadata metadata) {
super(type, server != null ? server.getUID() : new HomekitUID("server"), WILDCARD_UID, metadata);
this.server = Optional.ofNullable(server);
this.accessory = Optional.ofNullable(accessory);
this.service = Optional.ofNullable(service);
this.characteristic = Optional.ofNullable(characteristic);
}

    public Optional<HomekitAccessoryServer> getServer() {
        return server;
    }

    public Optional<HomekitAccessory> getAccessory() {
        return accessory;
    }

    public Optional<HomekitService> getService() {
        return service;
    }

    public Optional<HomekitCharacteristic<?>> getCharacteristic() {
        return characteristic;
    }

    @Override
    public String toString() {
        return "HomekitAccessoryServerEvent{" + "type=" + getType() + ", publisherUID=" + getPublisherUID() + ", timestamp="
                + getTimestamp() + ", server=" + server + ", accessory=" + accessory + ", service=" + service
                + ", characteristic=" + characteristic + '}';
    }
}
