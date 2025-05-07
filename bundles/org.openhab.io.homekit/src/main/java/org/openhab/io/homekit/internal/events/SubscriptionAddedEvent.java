package org.openhab.io.homekit.internal.events;

import java.util.Collections;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.UID;

@NonNullByDefault
public class SubscriptionAddedEvent extends AbstractHomekitEvent {
    private final HomekitEventSubscription subscription;

    public SubscriptionAddedEvent(HomekitEventSubscription subscription) {
        super(HomekitEventType.SUBSCRIPTION_ADDED, subscription.getPublisherUID(), WILDCARD_UID, new EventMetadata(subscription.getPublisherUID(), null, null, Collections.emptySet()));
        this.subscription = subscription;
    }

    public HomekitEventSubscription getSubscription() {
        return subscription;
    }

    @Override   
    public  UID getPublisherUID() {
        return subscription.getPublisherUID();
    }

    @Override
    public String toString() {
        return "SubscriptionAddedEvent{" + "subscription=" + subscription + ", type=" + getType() + ", timestamp="
                + getTimestamp() + '}';
    }
} 