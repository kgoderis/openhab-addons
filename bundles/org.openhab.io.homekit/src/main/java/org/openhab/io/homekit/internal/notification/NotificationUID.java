package org.openhab.io.homekit.internal.notification;

import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.core.thing.UID;

public class NotificationUID extends UID {

    // server id: accessory instance id : service id : characteristic instance id

    @Override
    protected int getMinimalNumberOfSegments() {
        return 4;
    }

    /**
     * Instantiates a new thing UID.
     *
     * @param serverId the server id
     * @param accessoryId the accessory instance id
     * @param serviceId the accessory instance id
     * @param characteristicId the characteristic instance id
     */
    public NotificationUID(String serverId, long accessoryId, long serviceId, long characteristicId) {
        super(serverId, Long.toString(accessoryId), Long.toString(serviceId), Long.toString(characteristicId));
    }

    public NotificationUID(@NonNull String string) {
        super(string);
    }

    /**
     * Returns the id.
     *
     * @return id the id
     */
    public String getId() {
        List<String> segments = getAllSegments();
        return segments.get(segments.size() - 1);
    }

}
