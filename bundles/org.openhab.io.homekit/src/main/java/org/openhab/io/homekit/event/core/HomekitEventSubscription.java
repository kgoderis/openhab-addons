package org.openhab.io.homekit.event.core;

import java.util.function.Predicate;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.Identifiable;
import org.openhab.core.thing.UID;
import org.openhab.io.homekit.api.event.HomekitEvent;
import org.openhab.io.homekit.api.event.HomekitEventSubscriber;
import org.openhab.io.homekit.api.event.HomekitEventType;
import org.openhab.io.homekit.util.HomekitUID;

/**
 * Represents a subscription to Homekit events.
 * <p>
 * This class encapsulates the details of an event subscription, including:
 * <ul>
 * <li>The type of events to receive</li>
 * <li>The publisher UID to receive events from</li>
 * <li>The subscriber that will receive the events</li>
 * <li>An optional filter predicate to further refine which events are received</li>
 * </ul>
 * </p>
 */
@NonNullByDefault
public class HomekitEventSubscription {
    public final HomekitEventType eventType;
    public final UID publisherUID;
    public final UID subscriberUID;
    public final HomekitEventSubscriber subscriber;
    public final Class<? extends HomekitEvent> expectedEventClass;
    public final Predicate<HomekitEvent> filter;
    protected volatile long lastEventTime;

    /**
     * Creates a new subscription with a custom filter predicate.
     *
     * @param eventType the type of events to receive
     * @param publisherUID the UID of the publisher to receive events from
     * @param subscriberUID the UID of the subscriber
     * @param subscriber the subscriber that will receive the events
     * @param expectedEventClass the expected class of events
     * @param filter a predicate to filter events
     */
    public HomekitEventSubscription(HomekitEventType eventType, UID publisherUID, UID subscriberUID,
            HomekitEventSubscriber subscriber, Class<? extends HomekitEvent> expectedEventClass,
            Predicate<HomekitEvent> filter) {
        this.eventType = eventType;
        this.publisherUID = publisherUID;
        this.subscriberUID = subscriberUID != null ? subscriberUID : generateSubscriberUid(subscriber);
        this.subscriber = subscriber;
        this.expectedEventClass = expectedEventClass;
        this.filter = filter;
        this.lastEventTime = System.currentTimeMillis();
    }

    /**
     * Creates a new subscription with a custom filter predicate.
     *
     * @param eventType the type of events to receive
     * @param publisherUID the UID of the publisher to receive events from
     * @param subscriber the subscriber that will receive the events
     * @param expectedEventClass the expected class of events
     * @param filter a predicate to filter events
     */
    public HomekitEventSubscription(HomekitEventType eventType, UID publisherUID, HomekitEventSubscriber subscriber,
            Class<? extends HomekitEvent> expectedEventClass, Predicate<HomekitEvent> filter) {
        this(eventType, publisherUID, null, subscriber, expectedEventClass, filter);
    }

    /**
     * Creates a new subscription with a custom filter predicate.
     *
     * @param eventType the type of events to receive
     * @param publisherUID the UID of the publisher to receive events from
     * @param subscriber the subscriber that will receive the events
     * @param filter a predicate to filter events
     */
    public HomekitEventSubscription(HomekitEventType eventType, UID publisherUID, HomekitEventSubscriber subscriber,
            Predicate<HomekitEvent> filter) {
        this(eventType, publisherUID, null, subscriber, HomekitEvent.class, filter);
    }

    /**
     * Creates a new subscription with default filter (accepts all events).
     *
     * @param eventType the type of events to receive
     * @param publisherUID the UID of the publisher to receive events from
     * @param subscriber the subscriber that will receive the events
     */
    public HomekitEventSubscription(HomekitEventType eventType, UID publisherUID, HomekitEventSubscriber subscriber) {
        this(eventType, publisherUID, null, subscriber, HomekitEvent.class, event -> true);
    }

    /**
     * Creates a new subscription with default filter (accepts all events).
     *
     * @param eventType the type of events to receive
     * @param publisherUID the UID of the publisher to receive events from
     * @param subscriberUID the UID of the subscriber
     * @param subscriber the subscriber that will receive the events
     */
    public HomekitEventSubscription(HomekitEventType eventType, UID publisherUID, UID subscriberUID,
            HomekitEventSubscriber subscriber) {
        this(eventType, publisherUID, subscriberUID, subscriber, HomekitEvent.class, event -> true);
    }

    /**
     * Creates a new subscription with default filter (accepts all events).
     *
     * @param eventType the type of events to receive
     * @param publisherUID the UID of the publisher to receive events from
     * @param subscriberUID the UID of the subscriber
     * @param subscriber the subscriber that will receive the events
     * @param expectedEventClass the expected class of events
     */
    public HomekitEventSubscription(HomekitEventType eventType, UID publisherUID, UID subscriberUID,
            HomekitEventSubscriber subscriber, Class<? extends HomekitEvent> expectedEventClass) {
        this(eventType, publisherUID, subscriberUID, subscriber, expectedEventClass, event -> true);
    }

    /**
     * Creates a new subscription with default filter (accepts all events).
     *
     * @param eventType the type of events to receive
     * @param publisherUID the UID of the publisher to receive events from
     * @param subscriber the subscriber that will receive the events
     * @param expectedEventClass the expected class of events
     */
    public HomekitEventSubscription(HomekitEventType eventType, UID publisherUID, HomekitEventSubscriber subscriber,
            Class<? extends HomekitEvent> expectedEventClass) {
        this(eventType, publisherUID, null, subscriber, expectedEventClass, event -> true);
    }

    public UID getPublisherUID() {
        return publisherUID;
    }

    public HomekitEventType getEventType() {
        return eventType;
    }

    public HomekitEventSubscriber getSubscriber() {
        return subscriber;
    }

    public UID getSubscriberUID() {
        return subscriberUID;
    }

    public Class<? extends HomekitEvent> getExpectedEventClass() {
        return expectedEventClass;
    }

    public long getLastEventTime() {
        return lastEventTime;
    }

    public void updateLastEventTime() {
        this.lastEventTime = System.currentTimeMillis();
    }

    /**
     * Generates a unique identifier for a subscriber.
     * If the subscriber implements Identifiable, uses its UID.
     * Otherwise, generates a hash-based identifier.
     *
     * @param subscriber the subscriber to generate an ID for
     * @return a unique identifier for the subscriber
     */
    private static UID generateSubscriberUid(HomekitEventSubscriber subscriber) {
        if (subscriber instanceof Identifiable) {
            Identifiable<?> identifiable = (Identifiable<?>) subscriber;
            if (identifiable.getUID() instanceof UID uid) {
                return uid;
            }
        }
        return new HomekitUID("homekit:subscriber:" + Integer.toHexString(System.identityHashCode(subscriber)));
    }
}
