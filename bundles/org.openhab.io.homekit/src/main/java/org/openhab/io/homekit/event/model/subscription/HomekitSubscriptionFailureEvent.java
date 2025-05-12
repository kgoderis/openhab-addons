package org.openhab.io.homekit.event.model.subscription;

import java.util.Collections;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.UID;
import org.openhab.io.homekit.api.event.HomekitEventType;
import org.openhab.io.homekit.event.core.AbstractHomekitEvent;
import org.openhab.io.homekit.event.core.HomekitEventMetadata;
import org.openhab.io.homekit.util.HomekitUID;

/**
 * Represents an event when a subscription fails in the Homekit integration.
 */
@NonNullByDefault
public class HomekitSubscriptionFailureEvent extends AbstractHomekitEvent {
    private final UID subscriberUid;
    private final HomekitEventType failedEventType;
    private final String errorMessage;
    private final Throwable cause;

    public HomekitSubscriptionFailureEvent(UID publisherUid, UID subscriberUid, HomekitEventType failedEventType,
            String errorMessage, Throwable cause) {
        super(HomekitEventType.SUBSCRIPTION_FAILED, publisherUid, HomekitUID.WILDCARD_UID,
                new HomekitEventMetadata(publisherUid, null, null, Collections.emptySet()));
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
        return "HomekitSubscriptionFailureEvent{" + "subscriberUid=" + subscriberUid + ", errorMessage='" + errorMessage
                + '\'' + ", failedEventType=" + failedEventType + ", timestamp=" + getTimestamp() + '}';
    }
}
