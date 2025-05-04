package org.openhab.io.homekit.internal.events;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public class SubscriptionAddedEvent extends AbstractHomekitEvent {
    private final HomekitEventSubscription subscription;

    public SubscriptionAddedEvent(HomekitEventSubscription subscription) {
        super(HomekitEventType.SUBSCRIPTION_ADDED, subscription.getPublisherUID());
        this.subscription = subscription;
    }

    public HomekitEventSubscription getSubscription() {
        return subscription;
    }

    @Override
    public String getPublisherUID() {
        return subscription.getPublisherUID();
    }

    @Override
    public String toString() {
        return "SubscriptionAddedEvent{" + "subscription=" + subscription + ", type=" + getType() + ", timestamp="
                + getTimestamp() + '}';
    }
} 