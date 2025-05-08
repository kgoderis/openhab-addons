package org.openhab.io.homekit.internal.events;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.UID;

/**
 * Interface for all HomeKit events.
 * <p>
 * Events include metadata for tracking propagation and preventing loops.
 * </p>
 */
@NonNullByDefault
public interface HomekitEvent {
    /**
     * Returns the type of this event.
     *
     * @return the event type
     */
    HomekitEventType getType();

    /**
     * Returns the UID of the publisher of this event.
     *
     * @return the publisher UID
     */
    UID getPublisherUID();

    /**
     * Sets the UID of the publisher of this event.
     * Note: This is typically only used when forwarding events.
     *
     * @param uid the publisher UID to set
     */
    void setPublisherUID(UID uid);

    /**
     * Returns the UID of the subscriber of this event.
     *
     * @return the subscriber UID, or null if not set
     */
    @Nullable
    UID getSubscriberUID();

    /**
     * Sets the UID of the subscriber of this event.
     *
     * @param uid the subscriber UID to set
     */
    void setSubscriberUID(UID uid);

    /**
     * Returns the timestamp when this event was created.
     *
     * @return the timestamp in milliseconds
     */
    long getTimestamp();

    /**
     * Returns the metadata for this event.
     *
     * @return the event metadata
     */
    HomekitEventMetadata getMetadata();

    @Override
    String toString();
}
