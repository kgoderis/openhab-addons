package org.openhab.io.homekit.event.model.service;

import java.util.Collections;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.UID;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristic;
import org.openhab.io.homekit.api.event.HomekitEventType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.event.core.AbstractHomekitEvent;
import org.openhab.io.homekit.event.core.HomekitEventMetadata;
import org.openhab.io.homekit.util.HomekitUID;

@NonNullByDefault
public class HomekitServiceEvent extends AbstractHomekitEvent {
    private final Optional<HomekitService> HomekitService;
    private final Optional<HomekitCharacteristic<?>> HomekitCharacteristic;

    @SuppressWarnings("null")
    public HomekitServiceEvent(HomekitEventType type, @Nullable HomekitService HomekitService, @Nullable HomekitCharacteristic<?> HomekitCharacteristic) {
        super(type, HomekitService != null ? HomekitService.getUID() : new HomekitUID("HomekitService"), WILDCARD_UID, new HomekitEventMetadata(HomekitService != null ? HomekitService.getUID() : new HomekitUID("HomekitService"), null, null, Collections.emptySet()));
        this.HomekitService = Optional.ofNullable(HomekitService);
        this.HomekitCharacteristic = Optional.ofNullable(HomekitCharacteristic);
    }

    @SuppressWarnings("null")
    public HomekitServiceEvent(HomekitEventType type,  @Nullable HomekitService HomekitService, UID subscriberUID, @Nullable HomekitCharacteristic<?> HomekitCharacteristic) {
        super(type, HomekitService != null ? HomekitService.getUID() : new HomekitUID("HomekitService"), subscriberUID, new HomekitEventMetadata(HomekitService != null ? HomekitService.getUID() : new HomekitUID("HomekitService"), null, null, Collections.emptySet()));
        this.HomekitService = Optional.ofNullable(HomekitService);
        this.HomekitCharacteristic = Optional.ofNullable(HomekitCharacteristic);
    }

    public Optional<HomekitService> getService() {
        return HomekitService;
    }

    public Optional<HomekitCharacteristic<?>> getCharacteristic() {
        return HomekitCharacteristic;
    }

    @Override
    public String toString() {
        return "HomekitServiceEvent{" + "type=" + getType() + ", publisherUID=" + getPublisherUID() + ", timestamp="
                + getTimestamp() + ", HomekitService=" + HomekitService + ", HomekitCharacteristic=" + HomekitCharacteristic + '}';
    }
}
