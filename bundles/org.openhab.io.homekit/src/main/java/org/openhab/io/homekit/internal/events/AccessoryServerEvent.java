package org.openhab.io.homekit.internal.events;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.hap.Accessory;
import org.openhab.io.homekit.api.hap.AccessoryServer;
import org.openhab.io.homekit.api.hap.Characteristic;
import org.openhab.io.homekit.api.hap.Service;

@NonNullByDefault
public class AccessoryServerEvent extends AbstractHomekitEvent {
    private final AccessoryServer server;
    private final Accessory accessory;
    private final Service service;
    private final Characteristic<?> characteristic;

    public AccessoryServerEvent(String sourceUid, HomekitEventType type, AccessoryServer server, Accessory accessory,
            Service service, Characteristic<?> characteristic) {
        super(sourceUid, type);
        this.server = server;
        this.accessory = accessory;
        this.service = service;
        this.characteristic = characteristic;
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

    @Override
    public String toString() {
        return "AccessoryServerEvent[type=" + getType() + ", server=" + server + ", accessory=" + accessory
                + ", service=" + service + ", characteristic=" + characteristic + "]";
    }
}
