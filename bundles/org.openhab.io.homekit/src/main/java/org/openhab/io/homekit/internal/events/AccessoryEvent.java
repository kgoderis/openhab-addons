package org.openhab.io.homekit.internal.events;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.io.homekit.api.hap.Accessory;
import org.openhab.io.homekit.api.hap.Characteristic;
import org.openhab.io.homekit.api.hap.Service;
import java.util.Optional;

@NonNullByDefault
public class AccessoryEvent extends AbstractHomekitEvent {

    private final Optional<Accessory> accessory;
    private final Optional<Service> service;
    private final Optional<Characteristic<?>> characteristic;
    
    public AccessoryEvent(HomekitEventType type, @Nullable Accessory accessory, @Nullable Service service, @Nullable Characteristic<?> characteristic) {
        super(accessory != null ? accessory.getUID().toString() : "unknown", type);
        this.accessory = Optional.ofNullable(accessory);
        this.service = Optional.ofNullable(service);
        this.characteristic = Optional.ofNullable(characteristic);
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
        return "AccessoryEvent{" +
                "type=" + getType() +
                ", sourceUid=" + getSourceUid() +
                ", timestamp=" + getTimestamp() +
                ", accessory=" + accessory +
                ", service=" + service +
                ", characteristic=" + characteristic +
                '}';
    }
}
