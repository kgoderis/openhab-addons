package org.openhab.io.homekit.internal.events;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.hap.Characteristic;
import org.openhab.io.homekit.api.hap.Service;

@NonNullByDefault
public class ServiceEvent extends AbstractHomekitEvent {
    private final Service service;
    private final Characteristic<?> characteristic;

    public ServiceEvent(HomekitEventType type, Service service, Characteristic<?> characteristic) {
        super(service.getUID().toString(), type);
        this.service = service;
        this.characteristic = characteristic;
    }

    public Service getService() {
        return service;
    }

    public Characteristic<?> getCharacteristic() {
        return characteristic;
    }

    @Override
    public String toString() {
        return "ServiceEvent[type=" + getType() + ", service=" + service + ", characteristic=" + characteristic + "]";
    }
}
