package org.openhab.io.homekit.internal.events;

import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.io.homekit.api.hap.Characteristic;
import org.openhab.io.homekit.api.hap.Service;

@NonNullByDefault
public class ServiceEvent extends AbstractHomekitEvent {
    private final Optional<Service> service;
    private final Optional<Characteristic<?>> characteristic;

    public ServiceEvent(HomekitEventType type, @Nullable Service service, @Nullable Characteristic<?> characteristic) {
        super(type, service != null ? service.getUID().toString() : "unknown");
        this.service = Optional.ofNullable(service);
        this.characteristic = Optional.ofNullable(characteristic);
    }

    public Optional<org.openhab.io.homekit.api.hap.Service> getService() {
        return service;
    }

    public Optional<Characteristic<?>> getCharacteristic() {
        return characteristic;
    }

    @Override
    public String toString() {
        return "ServiceEvent{" + "type=" + getType() + ", publisherUID=" + getPublisherUID() + ", timestamp="
                + getTimestamp() + ", service=" + service + ", characteristic=" + characteristic + '}';
    }
}
