package org.openhab.io.homekit.internal.events;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public class SubscriptionFailureEvent extends AbstractHomekitEvent {
    private final String subscriberUID;
    private final HomekitEventType eventType;
    private final String errorMessage;
    private final Throwable cause;

    public SubscriptionFailureEvent(String subscriberUID, String publisherUID, HomekitEventType eventType,
            String errorMessage, Throwable cause) {
        super(HomekitEventType.SUBSCRIPTION_FAILED, publisherUID);
        this.subscriberUID = subscriberUID;
        this.eventType = eventType;
        this.errorMessage = errorMessage;
        this.cause = cause;
    }

    public String getSubscriberUID() {
        return subscriberUID;
    }

    public HomekitEventType getFailedEventType() {
        return eventType;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Throwable getCause() {
        return cause;
    }

    @Override
    public String toString() {
        return "SubscriptionFailureEvent{" + "subscriberUID='" + subscriberUID + '\'' + ", publisherUID='" + getPublisherUID()
                + '\'' + ", eventType=" + eventType + ", errorMessage='" + errorMessage + '\'' + ", cause=" + cause
                + '}';
    }
} 