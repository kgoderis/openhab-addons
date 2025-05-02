package org.openhab.io.homekit.internal.events;

public class HomekitEventSubscription {
    protected final HomekitEventType eventType;
    protected final String publisherUID;
    protected final HomekitEventSubscriber subscriber;

    public HomekitEventSubscription(HomekitEventType eventType, String publisherUID,
            HomekitEventSubscriber subscriber) {
        this.eventType = eventType;
        this.publisherUID = publisherUID;
        this.subscriber = subscriber;
    }

    public String getPublisherUID() {
        return publisherUID;
    }

    public HomekitEventType getEventType() {
        return eventType;
    }

    public HomekitEventSubscriber getSubscriber() {
        return subscriber;
    }
}
