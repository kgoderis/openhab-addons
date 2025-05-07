package org.openhab.io.homekit.internal.events;

import java.util.Collections;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.UID;

/**
 * Represents an event when a subscription fails in the HomeKit integration.
 */
@NonNullByDefault
public class SubscriptionFailureEvent extends AbstractHomekitEvent {
    private final UID subscriberUid;
    private final HomekitEventType failedEventType;
    private final String errorMessage;
    private final Throwable cause;

    public SubscriptionFailureEvent(UID publisherUid, UID subscriberUid, HomekitEventType failedEventType,
            String errorMessage, Throwable cause) {
        super(HomekitEventType.SUBSCRIPTION_FAILED, publisherUid, HomekitUID.WILDCARD_UID, new EventMetadata(publisherUid, null, null, Collections.emptySet()));
        this.subscriberUid = subscriberUid;
        this.failedEventType = failedEventType;
        this.errorMessage = errorMessage;
        this.cause = cause;
    }

    public UID getSubscriberUid() {
        return subscriberUid;
    }

    public HomekitEventType getFailedEventType() {
        return failedEventType;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Throwable getCause() {
        return cause;
    }

    @Override
    public String toString() {
        return "SubscriptionFailureEvent{" + "subscriberUid=" + subscriberUid + ", errorMessage='" + errorMessage + '\''
                + ", failedEventType=" + failedEventType + ", timestamp=" + getTimestamp() + '}';
    }
} 