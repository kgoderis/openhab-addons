/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.openhab.io.homekit.event.core;

import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.UID;
import org.openhab.io.homekit.api.event.HomekitEvent;
import org.openhab.io.homekit.api.event.HomekitEventType;
import org.openhab.io.homekit.util.HomekitUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for HomeKit events in the OpenHAB HomeKit integration.
 *
 * <p>
 * This class provides the foundation for all HomeKit events, implementing common functionality
 * and enforcing event lifecycle rules. It manages event metadata, publisher/subscriber relationships,
 * and ensures proper event propagation through loop detection and hop count management.
 * </p>
 *
 * <p>
 * The class integrates with:
 * </p>
 * <ul>
 * <li>{@link HomekitEvent} for the event interface contract</li>
 * <li>{@link HomekitEventType} for event type definitions</li>
 * <li>{@link HomekitEventMetadata} for event lifecycle management</li>
 * <li>{@link org.openhab.core.thing.UID UID} for component identification</li>
 * </ul>
 *
 * <p>
 * <b>Key Features:</b>
 * </p>
 * <ul>
 * <li>Event type management</li>
 * <li>Publisher/subscriber routing</li>
 * <li>Event metadata tracking</li>
 * <li>Loop detection</li>
 * <li>Hop count management</li>
 * <li>Wildcard subscriber support</li>
 * <li>Timestamp tracking</li>
 * </ul>
 *
 * <p>
 * <b>Implementation Details:</b>
 * </p>
 * <ul>
 * <li>Uses immutable fields for thread safety</li>
 * <li>Implements proper logging for diagnostics</li>
 * <li>Supports wildcard subscribers</li>
 * <li>Validates event propagation</li>
 * <li>Manages event lifecycle</li>
 * </ul>
 *
 * <p>
 * <b>Security Considerations:</b>
 * </p>
 * <ul>
 * <li>Validates event propagation paths</li>
 * <li>Prevents infinite event loops</li>
 * <li>Enforces hop count limits</li>
 * <li>Maintains event integrity</li>
 * </ul>
 *
 * @author Karel Goderis - Initial contribution
 * @since 3.x
 */
@NonNullByDefault
public abstract class AbstractHomekitEvent implements HomekitEvent {
    private static final Logger logger = LoggerFactory.getLogger(AbstractHomekitEvent.class);
    private static final String LOG_PREFIX = "Homekit Event: ";

    private final HomekitEventType type;
    private final UID publisherUID;
    private UID subscriberUID;
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
     * @param subscriberUID the UID of the subscriber (if null, WILDCARD_UID will be used)
     * @param originalMetadata the metadata from the original event
     */
    protected AbstractHomekitEvent(HomekitEventType type, UID publisherUID, @Nullable UID subscriberUID,
            HomekitEventMetadata originalMetadata) {
        this.type = type;
        this.publisherUID = publisherUID;
        this.subscriberUID = subscriberUID != null ? subscriberUID : HomekitUID.WILDCARD_UID;
        this.metadata = new HomekitEventMetadata(originalMetadata, publisherUID);
        this.timestamp = System.currentTimeMillis();

        // Check for loops and hop count
        if (this.metadata.isInEventHistory(this.metadata.getEventId())) {
            logger.warn(
                    "{}Event loop detected - Event ID: {}, Type: {}, Publisher: {}, Hop Count: {}, Event History: {}",
                    LOG_PREFIX, this.metadata.getEventId(), type, publisherUID, this.metadata.getHopCount(),
                    this.metadata.getEventHistoryAsString());
            this.isValid = false;
        } else if (this.metadata.hasExceededMaxHops()) {
            logger.warn(
                    "{}Event exceeded maximum hop count - Event ID: {}, Type: {}, Publisher: {}, Hop Count: {}, Max Hops: {}, Event History: {}",
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

    /**
     * Gets the type of this event.
     * 
     * This method returns the event type that categorizes the event's purpose and behavior.
     * The event type is used for routing and filtering events in the system.
     *
     * @return The type of this event
     */
    @Override
    public HomekitEventType getType() {
        return type;
    }

    /**
     * Gets the unique identifier of the publisher that created this event.
     * 
     * This method returns the UID of the component that originally published this event.
     * The publisher UID is immutable and remains constant throughout the event's lifecycle.
     *
     * @return The UID of the event publisher
     */
    @Override
    public UID getPublisherUID() {
        return publisherUID;
    }

    /**
     * Attempts to set the publisher UID.
     * 
     * This method is a no-op as the publisher UID is immutable for security and traceability reasons.
     * The publisher UID is set only during event creation and cannot be changed afterward.
     *
     * @param uid The UID to set (ignored)
     */
    @Override
    public void setPublisherUID(UID uid) {
        // Publisher UID is immutable
    }

    /**
     * Gets the unique identifier of the subscriber for this event.
     * 
     * This method returns the UID of the component that should receive this event.
     * The subscriber UID will be WILDCARD_UID if the event is being broadcast.
     *
     * @return The UID of the event subscriber
     */
    @Override
    public Optional<UID> getSubscriberUID() {
        return Optional.of(subscriberUID);
    }

    /**
     * Sets the unique identifier of the subscriber for this event.
     * 
     * This method allows changing the target subscriber for this event.
     * If the UID is null, WILDCARD_UID will be used to indicate a broadcast event.
     *
     * @param uid The UID of the subscriber to set
     */
    @Override
    public void setSubscriberUID(@Nullable UID uid) {
        this.subscriberUID = uid != null ? uid : HomekitUID.WILDCARD_UID;
    }

    /**
     * Gets the metadata associated with this event.
     * 
     * This method returns the event metadata that contains information about the event's
     * lifecycle, propagation history, and correlation data. The metadata is used for
     * event tracking, loop detection, and diagnostics.
     *
     * @return The event metadata
     */
    @Override
    public HomekitEventMetadata getMetadata() {
        return metadata;
    }

    /**
     * Gets the timestamp when this event was created.
     * 
     * This method returns the creation time of the event in milliseconds since the epoch.
     * The timestamp is used for event ordering, correlation, and diagnostics.
     *
     * @return The event creation timestamp
     */
    @Override
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Returns whether this event is valid and should be processed.
     * 
     * An event is considered invalid if:
     * - It has been detected in an event loop
     * - It has exceeded the maximum allowed hop count
     * 
     * Invalid events should be dropped by the event system to prevent infinite loops
     * and excessive resource consumption.
     *
     * @return true if the event is valid and should be processed, false otherwise
     */
    public boolean isValid() {
        return isValid;
    }

    /**
     * Returns a string representation of this event.
     * 
     * This method provides a human-readable description of the event, including its
     * type, publisher, subscriber, and timestamp. The format is:
     * {className}{type=type, publisherUID=publisherUID, subscriberUID=subscriberUID, timestamp=timestamp}
     *
     * @return A string representation of this event
     */
    @Override
    public String toString() {
        return String.format("%s{type=%s, publisherUID=%s, subscriberUID=%s, timestamp=%d}", getClass().getSimpleName(),
                type, publisherUID, subscriberUID, timestamp);
    }
}
