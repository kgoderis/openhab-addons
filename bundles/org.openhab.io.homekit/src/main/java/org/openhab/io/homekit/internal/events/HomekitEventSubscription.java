package org.openhab.io.homekit.internal.events;

public class HomekitEventSubscription {
    public final HomekitEventType eventType;
    public final String sourceUid;
    public final HomekitEventSubscriber subscriber;

    public HomekitEventSubscription(HomekitEventType eventType, String sourceUid, HomekitEventSubscriber subscriber) {
        this.eventType = eventType;
        this.sourceUid = sourceUid;
        this.subscriber = subscriber;
    }
}
