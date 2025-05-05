package org.openhab.io.homekit.internal.events;

import org.eclipse.jdt.annotation.NonNullByDefault;

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
    String getPublisherUID();

    /**
     * Returns the metadata for this event.
     *
     * @return the event metadata
     */
    EventMetadata getMetadata();

    /**
     * Returns the timestamp when this event was created.
     *
     * @return the timestamp in milliseconds
     */
    long getTimestamp();

    void setPublisherUID(String uid);

    @Override
    String toString();

    String getSubscriberUID();

    void setSubscriberUID(String uid);
}
