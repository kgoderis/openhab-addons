package org.openhab.io.homekit.api.event;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.UID;
import org.openhab.io.homekit.event.core.HomekitEventMetadata;

/**
 * Interface for all HomeKit events in the system.
 *
 * This interface defines the core functionality for HomeKit events, providing a standardized
 * way to handle event propagation and tracking throughout the HomeKit integration. Events
 * are used to communicate state changes, commands, and other important notifications
 * between different components of the system.
 *
 * The interface provides:
 * - Event type identification
 * - Publisher and subscriber tracking
 * - Timestamp management
 * - Event metadata handling
 * - Loop prevention mechanisms
 *
 * Key implementation details:
 * - Thread-safe event handling
 * - Immutable event metadata
 * - Bidirectional event propagation
 * - Event correlation support
 *
 * The interface integrates with:
 * - {@link org.openhab.core.thing.UID} for component identification
 * - {@link org.openhab.io.homekit.event.core.HomekitEventMetadata} for event metadata
 * - {@link org.openhab.io.homekit.api.event.HomekitEventType} for event classification
 *
 * @author Karel Goderis - Initial Contribution
 * @since 1.0.0
 */
@NonNullByDefault
public interface HomekitEvent {
    /**
     * Gets the type of this event.
     * The event type determines how the event should be processed and handled.
     *
     * @return the event type
     * @since 1.0.0
     */
    HomekitEventType getType();

    /**
     * Gets the UID of the publisher of this event.
     * The publisher is the component that originally created and sent the event.
     *
     * @return the publisher UID
     * @since 1.0.0
     */
    UID getPublisherUID();

    /**
     * Sets the UID of the publisher of this event.
     * This method is typically only used when forwarding events to maintain
     * proper event propagation tracking.
     *
     * @param uid the publisher UID to set
     * @since 1.0.0
     */
    void setPublisherUID(UID uid);

    /**
     * Gets the UID of the subscriber of this event.
     * The subscriber is the component that will process this event.
     *
     * @return the subscriber UID, or null if not set
     * @since 1.0.0
     */
    @Nullable
    UID getSubscriberUID();

    /**
     * Sets the UID of the subscriber of this event.
     * This method is used to route events to specific components.
     *
     * @param uid the subscriber UID to set
     * @since 1.0.0
     */
    void setSubscriberUID(UID uid);

    /**
     * Gets the timestamp when this event was created.
     * The timestamp is used for event ordering and correlation.
     *
     * @return the timestamp in milliseconds
     * @since 1.0.0
     */
    long getTimestamp();

    /**
     * Gets the metadata for this event.
     * The metadata contains additional information about the event's context
     * and propagation history.
     *
     * @return the event metadata
     * @since 1.0.0
     */
    HomekitEventMetadata getMetadata();

    /**
     * Returns a string representation of this event.
     * The string includes essential event information for debugging and logging.
     *
     * @return a string representation of this event
     * @since 1.0.0
     */
    @Override
    String toString();
}
