package org.openhab.io.homekit.internal.events;

import java.util.Collections;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.UID;
import org.openhab.io.homekit.api.hap.Characteristic;
import org.openhab.io.homekit.api.hap.Service;

@NonNullByDefault
public class ServiceEvent extends AbstractHomekitEvent {
    private final Optional< Service> service;
    private final Optional< Characteristic<?>> characteristic;

    @SuppressWarnings("null")
    public ServiceEvent(HomekitEventType type, @Nullable Service service, @Nullable Characteristic<?> characteristic) {
        super(type, service != null ? service.getUID() : new HomekitUID("service"), WILDCARD_UID, new EventMetadata(service != null ? service.getUID() : new HomekitUID("service"), null, null, Collections.emptySet()));
        this.service = Optional.ofNullable(service);
        this.characteristic = Optional.ofNullable(characteristic);
    }

    @SuppressWarnings("null")
    public ServiceEvent(HomekitEventType type,  @Nullable Service service, UID subscriberUID, @Nullable Characteristic<?> characteristic) {
        super(type, service != null ? service.getUID() : new HomekitUID("service"), subscriberUID, new EventMetadata(service != null ? service.getUID() : new HomekitUID("service"), null, null, Collections.emptySet()));
        this.service = Optional.ofNullable(service);
        this.characteristic = Optional.ofNullable(characteristic);
    }

    public Optional< Service> getService() {
        return service;
    }

    public Optional< Characteristic<?>> getCharacteristic() {
        return characteristic;
    }

    @Override
    public String toString() {
        return "ServiceEvent{" + "type=" + getType() + ", publisherUID=" + getPublisherUID() + ", timestamp="
                + getTimestamp() + ", service=" + service + ", characteristic=" + characteristic + '}';
    }
}
