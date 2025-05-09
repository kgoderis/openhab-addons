package org.openhab.io.homekit.util;

import java.util.UUID;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.UID;

/**
 * Represents a unique identifier for a Homekit event handler.
 * This class provides a standardized way to generate and manage unique identifiers
 * for event handlers in the Homekit integration.
 * 
 * The UID format is: homekit:{prefix}:{uuid}
 * Example: homekit:bridge:550e8400-e29b-41d4-a716-446655440000
 */
@NonNullByDefault
public class HomekitUID extends UID {
    private static final String HOMEKIT_PREFIX = "homekit";
    private final String prefix;

    /** Wildcard UID that matches any subscriber */
    public static final UID WILDCARD_UID = new HomekitUID("*");

    /**
     * Creates a new HomekitUID with the specified prefix.
     * The actual UID will be in the format: homekit:{prefix}:{uuid}
     *
     * @param prefix The prefix to use for this handler UID (e.g., "bridge", "accessory", "service")
     */
    public HomekitUID(String prefix) {
        super(HOMEKIT_PREFIX, prefix, UUID.randomUUID().toString());
        this.prefix = prefix;
    }

    /**
     * Creates a new HomekitUID with the specified prefix and UID.
     * This constructor is useful when you need to recreate a UID from a stored value.
     * The UID must be in the format: homekit:{prefix}:{uuid}
     *
     * @param prefix The prefix to use for this handler UID
     * @param uid The unique identifier to use
     * @throws IllegalArgumentException if the UID format is invalid
     */
    public HomekitUID(String prefix, String uid) {
        super(uid);
        if (!uid.startsWith(HOMEKIT_PREFIX + ":" + prefix + ":")) {
            throw new IllegalArgumentException("Invalid UID format. Expected: homekit:" + prefix + ":{uuid}");
        }
        this.prefix = prefix;
    }

    /**
     * Returns the prefix of this handler UID.
     *
     * @return The prefix
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Returns the UUID part of this handler UID.
     *
     * @return The UUID
     */
    public String getUUID() {
        return getSegment(2);
    }

    @Override
    protected int getMinimalNumberOfSegments() {
        return 3; // homekit:prefix:uuid
    }
}
