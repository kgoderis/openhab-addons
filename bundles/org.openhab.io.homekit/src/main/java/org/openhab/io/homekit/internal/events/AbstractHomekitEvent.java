package org.openhab.io.homekit.internal.events;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.UID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for Homekit events.
 * <p>
 * Provides common functionality for all event types, including metadata tracking.
 * </p>
 */
@NonNullByDefault
public abstract class AbstractHomekitEvent implements HomekitEvent {
    private static final Logger logger = LoggerFactory.getLogger(AbstractHomekitEvent.class);
    private static final String LOG_PREFIX = "Homekit Event: ";

    /** Wildcard UID that matches any subscriber */
    public static final UID WILDCARD_UID = new HomekitUID("*");

    private final HomekitEventType type;
    private final UID publisherUID;
    private @Nullable UID subscriberUID;
    private final HomekitEventMetadata metadata;
    private final long timestamp;
    private final boolean isValid;

    /**
     * Creates a new event with the specified type, publisher, subscriber, and metadata.
     * Used when forwarding events to increment the hop count.
     * Performs loop and hop count checks.
     *
     * @param type the event type
     * @param publisherUID the UID of the publisher
     * @param subscriberUID the UID of the subscriber
     * @param originalMetadata the metadata from the original event
     */
    protected AbstractHomekitEvent(HomekitEventType type, UID publisherUID, UID subscriberUID,
            HomekitEventMetadata originalMetadata) {
        this.type = type;
        this.publisherUID = publisherUID;
        this.subscriberUID = subscriberUID;
        this.metadata = new HomekitEventMetadata(originalMetadata, publisherUID);
        this.timestamp = System.currentTimeMillis();

        // Check for loops and hop count
        if (this.metadata.isInEventHistory(this.metadata.getEventId())) {
            logger.warn(
                    "{}Event loop detected - Event ID: {}, Type: {}, Publisher: {}, Hop Count: {}\nEvent History: {}",
                    LOG_PREFIX, this.metadata.getEventId(), type, publisherUID, this.metadata.getHopCount(),
                    this.metadata.getEventHistoryAsString());
            this.isValid = false;
        } else if (this.metadata.hasExceededMaxHops()) {
            logger.warn(
                    "{}Event exceeded maximum hop count - Event ID: {}, Type: {}, Publisher: {}, Hop Count: {}, Max Hops: {}\nEvent History: {}",
                    LOG_PREFIX, this.metadata.getEventId(), type, publisherUID, this.metadata.getHopCount(),
                    HomekitEventMetadata.getMaxHops(), this.metadata.getEventHistoryAsString());
            this.isValid = false;
        } else {
            this.isValid = true;
        }
    }

    /**
     * Checks if a UID is a wildcard UID that matches any subscriber.
     *
     * @param uid the UID to check
     * @return true if the UID is a wildcard
     */
    public static boolean isWildcardUID(UID uid) {
        return HomekitUID.WILDCARD_UID.equals(uid);
    }

    /**
     * Generates a unique identifier for a publisher that doesn't have a UID.
     * The generated UID will be in the format: homekit:{publisherType}:{uuid}
     *
     * @param publisherType A string describing the type of publisher (e.g., "characteristic", "service")
     * @return A unique generated UID
     */
    protected static UID generatePublisherUID(String publisherType) {
        return new HomekitUID(publisherType);
    }

    @Override
    public HomekitEventType getType() {
        return type;
    }

    @Override
    public UID getPublisherUID() {
        return publisherUID;
    }

    @Override
    public void setPublisherUID(UID uid) {
        // Publisher UID is immutable
    }

    @Override
    public @Nullable UID getSubscriberUID() {
        return subscriberUID;
    }

    @Override
    public void setSubscriberUID(UID uid) {
        this.subscriberUID = uid;
    }

    @Override
    public HomekitEventMetadata getMetadata() {
        return metadata;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Returns whether this event is valid and should be processed.
     * An event is invalid if it has been detected in a loop or has exceeded the maximum hop count.
     *
     * @return true if the event is valid and should be processed
     */
    public boolean isValid() {
        return isValid;
    }

    @Override
    public String toString() {
        return String.format("%s{type=%s, publisherUID=%s, subscriberUID=%s, timestamp=%d}", getClass().getSimpleName(),
                type, publisherUID, subscriberUID, timestamp);
    }
}
