package org.openhab.io.homekit.internal.events;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for HomeKit events.
 * <p>
 * Provides common functionality for all event types, including metadata tracking.
 * </p>
 */
@NonNullByDefault
public abstract class AbstractHomekitEvent implements HomekitEvent {
    private static final Logger logger = LoggerFactory.getLogger(AbstractHomekitEvent.class);
    private static final String LOG_PREFIX = "HomeKit Event: ";
    
    private final HomekitEventType type;
    private String publisherUID;
    private final EventMetadata metadata;
    private final long timestamp;
    private final boolean isValid;

    /**
     * Creates a new event with the specified type and publisher.
     *
     * @param type the event type
     * @param publisherUID the UID of the publisher
     */
    protected AbstractHomekitEvent(HomekitEventType type, String publisherUID) {
        this.type = type;
        this.publisherUID = publisherUID;
        this.metadata = new EventMetadata(publisherUID, null, publisherUID, Set.of());
        this.timestamp = System.currentTimeMillis();
        this.isValid = true; // New events are always valid
    }

    /**
     * Creates a new event with the specified type and metadata.
     * Used when forwarding events to increment the hop count.
     * Performs loop and hop count checks.
     *
     * @param type the event type
     * @param publisherUID the UID of the publisher
     * @param originalMetadata the metadata from the original event
     */
    protected AbstractHomekitEvent(HomekitEventType type, String publisherUID, EventMetadata originalMetadata) {
        this.type = type;
        this.publisherUID = publisherUID;
        this.metadata = new EventMetadata(originalMetadata, publisherUID);
        this.timestamp = System.currentTimeMillis();
        
        // Check for loops and hop count
        if (this.metadata.isInHistory(this.metadata.getEventId())) {
            logger.warn("{}Event loop detected - Event ID: {}, Type: {}, Publisher: {}, Hop Count: {}\nEvent History: {}",
                    LOG_PREFIX, this.metadata.getEventId(), type, publisherUID, this.metadata.getHopCount(),
                    this.metadata.getEventHistoryAsString());
            this.isValid = false;
        } else if (this.metadata.hasExceededMaxHops()) {
            logger.warn("{}Event exceeded maximum hop count - Event ID: {}, Type: {}, Publisher: {}, Hop Count: {}, Max Hops: {}\nEvent History: {}",
                    LOG_PREFIX, this.metadata.getEventId(), type, publisherUID, this.metadata.getHopCount(),
                    EventMetadata.getMaxHops(), this.metadata.getEventHistoryAsString());
            this.isValid = false;
        } else {
            this.isValid = true;
        }
    }

    @Override
    public HomekitEventType getType() {
        return type;
    }

    @Override
    public String getPublisherUID() {
        return publisherUID;
    }

    @Override
    public void setPublisherUID(String uid) {
        this.publisherUID = uid;
    }

    @Override
    public EventMetadata getMetadata() {
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
    public abstract String toString();
}
