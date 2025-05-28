package org.openhab.io.homekit.event.model.subscription;

import java.util.Collections;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.UID;
import org.openhab.io.homekit.api.event.HomekitEventType;
import org.openhab.io.homekit.event.core.AbstractHomekitEvent;
import org.openhab.io.homekit.event.core.HomekitEventMetadata;
import org.openhab.io.homekit.util.HomekitUID;

/**
 * Event class representing a failure in HomeKit subscription operations.
 * This event is fired when an error occurs during subscription management,
 * such as failed subscription attempts or subscription processing errors.
 *
 * <p>
 * The class integrates with:
 * </p>
 * <ul>
 * <li>{@link org.openhab.io.homekit.api.event.HomekitEvent} for base event functionality</li>
 * <li>{@link org.openhab.io.homekit.api.event.HomekitEventType} for event type identification</li>
 * <li>{@link org.openhab.core.thing.UID} for component identification</li>
 * <li>{@link org.openhab.io.homekit.event.core.HomekitEventMetadata} for event metadata</li>
 * </ul>
 *
 * <p>
 * <b>Key Features:</b>
 * </p>
 * <ul>
 * <li>Detailed error tracking with message and cause</li>
 * <li>Subscriber identification for targeted error handling</li>
 * <li>Failed event type tracking for diagnostics</li>
 * <li>Timestamp-based error correlation</li>
 * </ul>
 *
 * <p>
 * <b>Usage Patterns:</b>
 * </p>
 * <ul>
 * <li>Error handling in subscription management</li>
 * <li>Diagnostic logging and monitoring</li>
 * <li>Error recovery and retry mechanisms</li>
 * <li>Subscription state management</li>
 * </ul>
 *
 * <p>
 * <b>Error Handling:</b>
 * </p>
 * <ul>
 * <li>Captures both error messages and underlying causes</li>
 * <li>Maintains subscriber context for targeted recovery</li>
 * <li>Supports error correlation through event types</li>
 * <li>Enables diagnostic analysis through metadata</li>
 * </ul>
 *
 * @author Karel Goderis - Initial contribution
 * @since 3.x
 */
@NonNullByDefault
public class HomekitSubscriptionFailureEvent extends AbstractHomekitEvent {
    private final UID subscriberUid;
    private final HomekitEventType failedEventType;
    private final String errorMessage;
    private final Throwable cause;

    /**
     * Creates a new subscription failure event.
     *
     * @param publisherUid the UID of the publisher that generated the event
     * @param subscriberUid the UID of the subscriber that encountered the failure
     * @param failedEventType the type of event that failed
     * @param errorMessage a descriptive message about the failure
     * @param cause the throwable that caused the failure
     */
    public HomekitSubscriptionFailureEvent(UID publisherUid, UID subscriberUid, HomekitEventType failedEventType,
            String errorMessage, Throwable cause) {
        super(HomekitEventType.SUBSCRIPTION_FAILED, publisherUid, HomekitUID.WILDCARD_UID,
                new HomekitEventMetadata(publisherUid, null, null, Collections.emptySet()));
        this.subscriberUid = subscriberUid;
        this.failedEventType = failedEventType;
        this.errorMessage = errorMessage;
        this.cause = cause;
    }

    /**
     * Returns the UID of the subscriber that encountered the failure.
     *
     * @return the subscriber UID
     */
    public UID getSubscriberUid() {
        return subscriberUid;
    }

    /**
     * Returns the type of event that failed.
     *
     * @return the failed event type
     */
    public HomekitEventType getFailedEventType() {
        return failedEventType;
    }

    /**
     * Returns the error message describing the failure.
     *
     * @return the error message
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Returns the throwable that caused the failure.
     *
     * @return the cause of the failure
     */
    public Throwable getCause() {
        return cause;
    }

    @Override
    public String toString() {
        return "HomekitSubscriptionFailureEvent{" + "subscriberUid=" + subscriberUid + ", errorMessage='" + errorMessage
                + '\'' + ", failedEventType=" + failedEventType + ", timestamp=" + getTimestamp() + '}';
    }
}
