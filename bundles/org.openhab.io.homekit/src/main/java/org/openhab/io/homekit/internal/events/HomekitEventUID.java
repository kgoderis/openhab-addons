package org.openhab.io.homekit.internal.events;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.UID;

/**
 * Represents a unique identifier for a Homekit event.
 * The UID format is: homekit:event:{eventType}:{uuid}
 */
@NonNullByDefault
public class HomekitEventUID extends UID {
    private static final String HOMEKIT_PREFIX = "homekit";
    private static final String EVENT_PREFIX = "event";
    private final HomekitEventType eventType;

    /**
     * Creates a new EventUID with the specified event type.
     * The actual UID will be in the format: homekit:event:{eventType}:{uuid}
     *
     * @param eventType The type of event this UID represents
     */
    public HomekitEventUID(HomekitEventType eventType) {
        super(HOMEKIT_PREFIX, EVENT_PREFIX, eventType.name(), java.util.UUID.randomUUID().toString());
        this.eventType = eventType;
    }

    /**
     * Creates a new EventUID from an existing UID string.
     * The UID must be in the format: homekit:event:{eventType}:{uuid}
     *
     * @param uid The existing UID string
     * @throws IllegalArgumentException if the UID format is invalid
     */
    public HomekitEventUID(String uid) {
        super(uid);
        if (!uid.startsWith(HOMEKIT_PREFIX + ":" + EVENT_PREFIX + ":")) {
            throw new IllegalArgumentException("Invalid UID format. Expected: homekit:event:{eventType}:{uuid}");
        }
        this.eventType = HomekitEventType.valueOf(getSegment(2));
    }

    /**
     * Returns the event type of this UID.
     *
     * @return The event type
     */
    public HomekitEventType getEventType() {
        return eventType;
    }

    /**
     * Returns the UUID part of this UID.
     *
     * @return The UUID
     */
    public String getUUID() {
        return getSegment(3);
    }

    @Override
    protected int getMinimalNumberOfSegments() {
        return 4; // homekit:event:eventType:uuid
    }
}
