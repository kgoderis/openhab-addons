package org.openhab.io.homekit.internal.events;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public class SubscriptionRemovedEvent extends AbstractHomekitEvent {
    private final HomekitEventSubscription subscription;

    public SubscriptionRemovedEvent(HomekitEventSubscription subscription) {
        super(HomekitEventType.SUBSCRIPTION_REMOVED, subscription.getPublisherUID());
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
        return "SubscriptionRemovedEvent{" + "subscription=" + subscription + ", type=" + getType() + ", timestamp="
                + getTimestamp() + '}';
    }
} 