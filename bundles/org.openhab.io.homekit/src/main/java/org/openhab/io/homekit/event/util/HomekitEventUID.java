package org.openhab.io.homekit.event.util;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.UID;
import org.openhab.io.homekit.api.event.HomekitEventType;

/**
 * Implementation of unique identifiers for HomeKit events in the OpenHAB system.
 * This class extends {@link org.openhab.core.thing.UID} to provide specialized UID handling
 * for HomeKit events, ensuring consistent identification and type safety across the event system.
 *
 * The class integrates with:
 * - {@link org.openhab.core.thing.UID OpenHAB's UID system} for base UID functionality
 * - {@link HomekitEventType} for event type enumeration
 * - {@link java.util.UUID} for generating unique identifiers
 *
 * UID Structure:
 * - Format: homekit:event:{eventType}:{uuid}
 * - Prefix: "homekit" - Identifies the HomeKit domain
 * - Category: "event" - Specifies the UID category
 * - Event Type: Enum value from {@link HomekitEventType}
 * - UUID: Random UUID for uniqueness
 *
 * Key Features:
 * - Type-safe event identification
 * - Consistent UID format
 * - UUID-based uniqueness
 * - Event type validation
 * - Format validation
 *
 * Usage Patterns:
 * - Creating new event UIDs: Use {@link #HomekitEventUID(HomekitEventType)}
 * - Parsing existing UIDs: Use {@link #HomekitEventUID(String)}
 * - Retrieving event type: Use {@link #getEventType()}
 * - Accessing UUID: Use {@link #getUUID()}
 *
 * Validation Rules:
 * - UID must start with "homekit:event:"
 * - Event type must be a valid enum value
 * - UUID must be present and valid
 * - Minimum of 4 segments required
 *
 * @author Karel Goderis - Initial contribution
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
     * @throws IllegalArgumentException if eventType is null
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
     * @throws IllegalArgumentException if the UID format is invalid or if the event type is unknown
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
     * The event type determines the category and behavior of the event in the system.
     *
     * @return The event type
     */
    public HomekitEventType getEventType() {
        return eventType;
    }

    /**
     * Returns the UUID part of this UID.
     * The UUID ensures uniqueness of the event identifier.
     *
     * @return The UUID
     */
    public String getUUID() {
        return getSegment(3);
    }

    /**
     * Specifies the minimum number of segments required in a valid event UID.
     * The format is: homekit:event:eventType:uuid
     *
     * @return The minimum number of segments (4)
     */
    @Override
    protected int getMinimalNumberOfSegments() {
        return 4; // homekit:event:eventType:uuid
    }
}
