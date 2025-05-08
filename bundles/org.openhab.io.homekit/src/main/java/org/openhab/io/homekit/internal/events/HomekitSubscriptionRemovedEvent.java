package org.openhab.io.homekit.internal.events;

import java.util.Collections;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.UID;

@NonNullByDefault
public class HomekitSubscriptionRemovedEvent extends AbstractHomekitEvent {
    private final HomekitEventSubscription subscription;

    public HomekitSubscriptionRemovedEvent(HomekitEventSubscription subscription) {
        super(HomekitEventType.SUBSCRIPTION_REMOVED, subscription.getPublisherUID(), WILDCARD_UID, new HomekitEventMetadata(subscription.getPublisherUID(), null, null, Collections.emptySet()));
        this.subscription = subscription;
    }

    public HomekitEventSubscription getSubscription() {
        return subscription;
    }

    @Override
    public UID getPublisherUID() {
        return subscription.getPublisherUID();
    }

    @Override
    public String toString() {
        return "HomekitSubscriptionRemovedEvent{" + "subscription=" + subscription + ", type=" + getType() + ", timestamp="
                + getTimestamp() + '}';
    }
}
