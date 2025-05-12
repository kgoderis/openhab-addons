package org.openhab.io.homekit.event.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.common.registry.Identifiable;
import org.openhab.core.thing.UID;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.event.HomekitEvent;
import org.openhab.io.homekit.api.event.HomekitEventSubscriber;
import org.openhab.io.homekit.api.event.HomekitEventType;
import org.openhab.io.homekit.api.registry.HomekitAccessoryRegistry;
import org.openhab.io.homekit.core.accessory.HomekitAccessoryUID;
import org.openhab.io.homekit.event.core.AbstractHomekitEvent;
import org.openhab.io.homekit.event.core.HomekitEventSubscription;
import org.openhab.io.homekit.event.model.accessory.HomekitAccessoryEvent;
import org.openhab.io.homekit.event.model.characteristic.HomekitCharacteristicEvent;
import org.openhab.io.homekit.event.model.service.HomekitServiceEvent;
import org.openhab.io.homekit.event.model.subscription.HomekitSubscriptionAddedEvent;
import org.openhab.io.homekit.event.model.subscription.HomekitSubscriptionFailureEvent;
import org.openhab.io.homekit.event.model.subscription.HomekitSubscriptionRemovedEvent;
import org.openhab.io.homekit.event.util.HomekitEventLogger;
import org.openhab.io.homekit.util.HomekitUID;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages Homekit event publishing and subscription for the OpenHAB Homekit integration.
 * <p>
 * This class is responsible for:
 * <ul>
 * <li>Maintaining a registry of event subscribers (using weak references for memory safety)</li>
 * <li>Queuing and dispatching events to subscribers asynchronously</li>
 * <li>Handling event retries, timeouts, and error notification</li>
 * <li>Cleaning up dead subscribers automatically</li>
 * <li>Registering a singleton event logger for all event types</li>
 * </ul>
 * <p>
 * The manager is thread-safe and designed to be used as an OSGi component.
 * </p>
 *
 * <h2>Wildcard and Prefix Support</h2>
 * <p>
 * The event manager supports flexible subscription patterns through wildcards and prefixes:
 * </p>
 * <ul>
 * <li><b>Wildcard ("*")</b>: Matches any publisher UID or event type
 * <ul>
 * <li>Subscribe to all events: {@code subscribe(HomekitEventType.ANY, "*", subscriber)}</li>
 * <li>Subscribe to all events from a specific publisher:
 * {@code subscribe(HomekitEventType.ANY, "publisherId", subscriber)}</li>
 * <li>Subscribe to a specific event type from all publishers: {@code subscribe(eventType, "*", subscriber)}</li>
 * </ul>
 * </li>
 * <li><b>Prefix Matching</b>: Matches publisher UIDs that start with a specific prefix
 * <ul>
 * <li>Subscribe to events from all accessories: {@code subscribe(eventType, "accessory:*", subscriber)}</li>
 * <li>Subscribe to events from a specific server: {@code subscribe(eventType, "server:123:*", subscriber)}</li>
 * </ul>
 * </li>
 * </ul>
 *
 * <h2>Error Handling</h2>
 * <p>
 * The event manager provides comprehensive error handling:
 * </p>
 * <ul>
 * <li><b>Retry Mechanism</b>:
 * <ul>
 * <li>Events are retried up to {@link #MAX_RETRIES} times</li>
 * <li>Uses exponential backoff between retries</li>
 * <li>Retry delay increases with each attempt: {@code RETRY_DELAY_MS * 2^retryCount}</li>
 * </ul>
 * </li>
 * <li><b>Error Notification</b>:
 * <ul>
 * <li>Subscribers are notified of failures through {@link HomekitEventSubscriber#onEventError}</li>
 * <li>System-wide error monitoring through {@link HomekitEventType#SUBSCRIPTION_FAILED} events</li>
 * <li>Detailed error information including subscriber, publisher, event type, and cause</li>
 * </ul>
 * </li>
 * <li><b>Failure Recovery</b>:
 * <ul>
 * <li>Subscriptions are automatically removed after max retries exceeded</li>
 * <li>Dead subscribers are cleaned up automatically</li>
 * <li>Orphaned subscriptions are detected and logged</li>
 * </ul>
 * </li>
 * </ul>
 *
 * <h2>Advanced Subscription Options</h2>
 * <p>
 * The event manager supports several advanced subscription features:
 * </p>
 * <ul>
 * <li><b>Bulk Subscriptions</b>:
 * <ul>
 * <li>Subscribe to multiple event types at once:
 * {@code subscribe(Set<HomekitEventType>, publisherUID, subscriber)}</li>
 * <li>Bulk unsubscribe all subscriptions for a subscriber: {@code unsubscribeAllBySubscriberUid}</li>
 * <li>Bulk unsubscribe all subscriptions for a publisher: {@code unsubscribeAllByPublisherUid}</li>
 * </ul>
 * </li>
 * <li><b>Event Type Filtering</b>:
 * <ul>
 * <li>Subscribe to specific event types: {@code subscribe(eventType, publisherUID, subscriber)}</li>
 * <li>Subscribe to event type categories:
 * {@code subscribe(HomekitEventType.CHARACTERISTIC_ANY, publisherUID, subscriber)}</li>
 * <li>Subscribe to all events: {@code subscribe(HomekitEventType.ANY, publisherUID, subscriber)}</li>
 * </ul>
 * </li>
 * <li><b>Custom Event Classes</b>:
 * <ul>
 * <li>Specify expected event class for type safety:
 * {@code subscribe(eventType, publisherUID, subscriber, expectedEventClass)}</li>
 * <li>Use custom event handlers:
 * {@code subscribe(eventType, publisherUID, subscriberUID, subscriber, handler, expectedEventClass)}</li>
 * </ul>
 * </li>
 * <li><b>Subscription Management</b>:
 * <ul>
 * <li>Query subscriptions by subscriber: {@code getSubscriptionsBySubscriberUid}</li>
 * <li>Query subscriptions by publisher: {@code getSubscriptionsByPublisherUid}</li>
 * <li>Query subscriptions by event type: {@code getSubscriptionsByEventType}</li>
 * <li>Get subscription counts: {@code getSubscriptionCountBySubscriberUid},
 * {@code getSubscriptionCountByPublisherUid}</li>
 * </ul>
 * </li>
 * </ul>
 *
 * @author
 * @since 3.x
 */
@NonNullByDefault
@Component(service = HomekitEventManager.class)
public class HomekitEventManager {
    private static final String THREAD_POOL_NAME = "homekit-event-manager";
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;
    private static final long EVENT_TIMEOUT_MS = 5000;
    private static final int MAX_QUEUE_SIZE = 1000;
    private static final long QUEUE_PROCESSING_DELAY_MS = 100;
    private static final long SHUTDOWN_TIMEOUT_MS = 10000; // 10 seconds
    private static final int MAX_EVENT_HISTORY = 100;

    private static final Logger logger = LoggerFactory.getLogger(HomekitEventManager.class);
    private static final String LOG_PREFIX = "Homekit EventManager: ";
    private static final String LOG_EVENT = LOG_PREFIX + "Event - ";
    private static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    private static final String LOG_WARN = LOG_PREFIX + "Warning - ";
    private static final String LOG_SUBSCRIBER = LOG_PREFIX + "Subscriber - ";
    private static final String LOG_QUEUE = LOG_PREFIX + "Queue - ";
    private static final String LOG_INIT = LOG_PREFIX + "Init - ";
    private static final String LOG_PUBLISHER = LOG_PREFIX + "Publisher - ";
    private static final String LOG_METRICS = LOG_PREFIX + "Metrics - ";

    private final Set<HomekitEventSubscription> subscriptions = ConcurrentHashMap.newKeySet();
    private final PriorityBlockingQueue<HomekitEvent> eventQueue = new PriorityBlockingQueue<>(MAX_QUEUE_SIZE,
            (e1, e2) -> Long.compare(e1.getTimestamp(), e2.getTimestamp()));
    private final ScheduledExecutorService eventExecutor;
    private final AtomicBoolean isRunning = new AtomicBoolean(true);
    private final AtomicInteger activeEventCount = new AtomicInteger(0);
    private final HomekitEventLogger eventLogger;
    private final Set<UID> activePublishers = ConcurrentHashMap.newKeySet();

    // Metrics counters
    private final AtomicInteger droppedEventsDueToLoops = new AtomicInteger(0);
    private final AtomicInteger droppedEventsDueToHops = new AtomicInteger(0);
    private final AtomicInteger droppedEventsDueToCorrelation = new AtomicInteger(0);
    private final AtomicInteger totalEventsProcessed = new AtomicInteger(0);
    private final AtomicInteger totalEventsPublished = new AtomicInteger(0);

    private final HomekitAccessoryRegistry accessoryRegistry;

    @FunctionalInterface
    public interface HomekitEventHandler {
        void onEvent(HomekitEvent event);

        default void onError(HomekitEvent event, Exception e) {
            // Default error handling - do nothing
        }
    }

    /**
     * Constructs and activates the Homekit Event Manager.
     * Initializes the event processor.
     */
    @Activate
    public HomekitEventManager(@Reference HomekitAccessoryRegistry accessoryRegistry) {
        this.accessoryRegistry = accessoryRegistry;
        this.eventExecutor = ThreadPoolManager.getScheduledPool(THREAD_POOL_NAME);
        this.eventLogger = HomekitEventLogger.getInstance();
        registerEventLogger();
        startEventProcessor();
        logger.debug("{}Activated", LOG_INIT);
    }

    /**
     * Deactivates the event manager, shutting down all background processing and clearing subscribers.
     */
    @Deactivate
    public void deactivate() {
        isRunning.set(false);
        shutdown();
        logger.debug("{}Deactivated", LOG_INIT);
    }

    // Register the logger as a regular subscription for all event types and all sources
    private void registerEventLogger() {
        for (HomekitEventType type : HomekitEventType.values()) {
            subscribe(type, HomekitUID.WILDCARD_UID, HomekitEventLogger.getInstance(), HomekitEvent.class,
                    event -> true);
        }
    }

    /**
     * Starts the background event processor that dispatches queued events to subscribers.
     */
    private void startEventProcessor() {
        eventExecutor.scheduleWithFixedDelay(() -> {
            if (!isRunning.get()) {
                return;
            }
            try {
                processEvents();
            } catch (Exception e) {
                logger.error("{}Error processing events: {}", LOG_ERROR, e.getMessage(), e);
            }
        }, 0, QUEUE_PROCESSING_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Processes all events currently in the queue, dispatching them to the appropriate subscribers.
     */
    private void processEvents() {
        @Nullable
        HomekitEvent event;
        while (isRunning.get() && (event = eventQueue.poll()) != null) {
            activeEventCount.incrementAndGet();
            try {
                publishEventToSubscribers(event, 0, false);
            } finally {
                activeEventCount.decrementAndGet();
            }
        }
    }

    /**
     * Publishes an event to the event queue for asynchronous delivery to subscribers.
     * If the queue is full, the oldest event is discarded to make room.
     *
     * @param event the event to publish
     */
    public void publishEvent(HomekitEvent event) {
        if (!isRunning.get()) {
            logger.warn("{}Event manager is shutting down, event discarded", LOG_WARN);
            return;
        }

        totalEventsPublished.incrementAndGet();

        // Check if event is valid (loop and hop count checks are now done in the event constructor)
        if (event instanceof AbstractHomekitEvent) {
            AbstractHomekitEvent abstractEvent = (AbstractHomekitEvent) event;
            if (!abstractEvent.isValid()) {
                if (abstractEvent.getMetadata().isInEventHistory(abstractEvent.getMetadata().getEventId())) {
                    droppedEventsDueToLoops.incrementAndGet();
                } else if (abstractEvent.getMetadata().hasExceededMaxHops()) {
                    droppedEventsDueToHops.incrementAndGet();
                }
                return; // Event is invalid, drop it
            }
        }

        // Check correlation ID for loops using HomekitEventMetadata
        UID correlationId = event.getMetadata().getCorrelationId();
        if (correlationId != null) {
            if (event.getMetadata().hasProcessedCorrelationId(correlationId)) {
                logger.debug("{}Event with correlation ID {} already processed, skipping", LOG_EVENT, correlationId);
                droppedEventsDueToCorrelation.incrementAndGet();
                return;
            }
            event.getMetadata().addProcessedCorrelationId(correlationId);
        }

        UID publisherUID = event.getPublisherUID();
        activePublishers.add(publisherUID);

        try {
            boolean queued = false;
            while (!queued && isRunning.get()) {
                if (eventQueue.size() >= MAX_QUEUE_SIZE) {
                    @Nullable
                    HomekitEvent oldestEvent = eventQueue.poll();
                    if (oldestEvent != null) {
                        logger.warn("{}Event queue is full, oldest event discarded: type={}, timestamp={}", LOG_QUEUE,
                                oldestEvent.getType(), oldestEvent.getTimestamp());
                    }
                }
                queued = eventQueue.offer(event, 100, TimeUnit.MILLISECONDS);
            }

            if (queued) {
                logger.debug(
                        "{}Event queued: type={}, publisherUID={}, subscriberUID={}, timestamp={}, eventId={}, hopCount={}",
                        LOG_EVENT, event.getType(), publisherUID, event.getSubscriberUID(), event.getTimestamp(),
                        event.getMetadata().getEventId(), event.getMetadata().getHopCount());
            } else {
                logger.warn("{}Failed to queue event after timeout, event discarded", LOG_QUEUE);
            }
        } catch (Exception e) {
            logger.error("{}Error queuing event: {}", LOG_ERROR, e.getMessage(), e);
        }
    }

    private boolean matchesPublisherPattern(UID pattern, UID publisherUID) {
        if (HomekitUID.WILDCARD_UID.equals(pattern)) {
            return true;
        }
        String[] patternParts = pattern.toString().split(":");
        String[] uidParts = publisherUID.toString().split(":");

        if (patternParts.length > uidParts.length) {
            return false;
        }

        for (int i = 0; i < patternParts.length; i++) {
            if ("*".equals(patternParts[i])) {
                continue;
            }
            if (!patternParts[i].equals(uidParts[i])) {
                return false;
            }
        }
        return true;
    }

    private CompletableFuture<Void> publishEventToSubscribers(HomekitEvent event, int retryCount, boolean retry) {
        List<HomekitEventSubscription> matchingSubs = new ArrayList<>();

        // Get the destination UID from the event metadata if present
        UID destinationUID = event.getSubscriberUID();

        // If we have a specific destination, only match subscriptions for that subscriber
        if (destinationUID != null && !destinationUID.equals(HomekitUID.WILDCARD_UID)) {
            matchingSubs = getSubscriptionsBySubscriberUid(destinationUID).stream()
                    .filter(sub -> sub.eventType.matches(event.getType())
                            && sub.getExpectedEventClass().isAssignableFrom(event.getClass()) && sub.filter.test(event))
                    .collect(Collectors.toList());
        } else {
            // Broadcast case - match all relevant subscriptions
            for (HomekitEventSubscription sub : subscriptions) {
                if (sub.eventType.matches(event.getType())
                        && matchesPublisherPattern(sub.publisherUID, event.getPublisherUID())
                        && sub.getExpectedEventClass().isAssignableFrom(event.getClass()) && sub.filter.test(event)) {
                    matchingSubs.add(sub);
                }
            }
        }

        List<CompletableFuture<Void>> futures = createSubscriberFutures(event, matchingSubs, retryCount, retry);

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .orTimeout(EVENT_TIMEOUT_MS, TimeUnit.MILLISECONDS).exceptionally(throwable -> {
                    logger.error("{}Error during synchronized event publishing: {}", LOG_ERROR, throwable.getMessage(),
                            throwable);
                    return null;
                });
    }

    private List<CompletableFuture<Void>> createSubscriberFutures(HomekitEvent event,
            List<HomekitEventSubscription> subs, int retryCount, boolean retry) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (HomekitEventSubscription sub : subs) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    sub.subscriber.onEvent(event);
                    sub.updateLastEventTime();
                } catch (Exception e) {
                    handleEventError(event, sub.subscriber, retryCount, e);
                }
            }, eventExecutor).orTimeout(EVENT_TIMEOUT_MS, TimeUnit.MILLISECONDS).exceptionally(e -> {
                logger.error("{}Event timeout or error for source UID {}: {}", LOG_ERROR, event.getPublisherUID(),
                        e.getMessage());
                handleEventError(event, sub.subscriber, MAX_RETRIES, new TimeoutException(e.getMessage()));
                return null;
            });
            futures.add(future);
        }
        return futures;
    }

    private HomekitEventSubscription findExistingSubscription(HomekitEventType eventType, UID publisherUID,
            HomekitEventSubscriber subscriber) {
        return subscriptions.stream().filter(sub -> sub.eventType == eventType && sub.publisherUID.equals(publisherUID)
                && sub.subscriber == subscriber).findFirst().orElse(null);
    }

    private HomekitEventSubscription createAndAddSubscription(HomekitEventType eventType, UID publisherUID,
            UID subscriberUID, HomekitEventSubscriber subscriber,
            @Nullable Class<? extends HomekitEvent> expectedEventClass) {
        Class<? extends HomekitEvent> eventClass = expectedEventClass != null ? expectedEventClass
                : switch (eventType) {
                    case CHARACTERISTIC_VALUE_CHANGED, CHARACTERISTIC_ADDED, CHARACTERISTIC_REMOVED ->
                        HomekitCharacteristicEvent.class;
                    case SERVICE_STATE_CHANGED, SERVICE_ADDED, SERVICE_REMOVED -> HomekitServiceEvent.class;
                    case ACCESSORY_STATE_CHANGED, ACCESSORY_ADDED, ACCESSORY_REMOVED -> HomekitAccessoryEvent.class;
                    case SUBSCRIPTION_ADDED, SUBSCRIPTION_REMOVED -> HomekitEvent.class;
                    default -> HomekitEvent.class;
                };

        HomekitEventSubscription subscription = subscriberUID != null
                ? new HomekitEventSubscription(eventType, publisherUID, subscriberUID, subscriber, eventClass)
                : new HomekitEventSubscription(eventType, publisherUID, subscriber, eventClass);
        if (subscriptions.add(subscription)) {
            publishEvent(new HomekitSubscriptionAddedEvent(subscription));
            logger.debug("{}Subscriber added for event type {} and source UID {}", LOG_SUBSCRIBER, eventType,
                    publisherUID);
        }
        return subscription;
    }

    public HomekitEventSubscription subscribe(HomekitEventType eventType, UID publisherUID,
            HomekitEventSubscriber subscriber) {
        HomekitEventSubscription subscription = new HomekitEventSubscription(eventType, publisherUID, subscriber);
        if (subscriptions.add(subscription)) {
            publishEvent(new HomekitSubscriptionAddedEvent(subscription));
            logger.debug("{}Subscriber added for event type {} and source UID {}", LOG_SUBSCRIBER, eventType,
                    publisherUID);
        }
        return subscription;
    }

    public HomekitEventSubscription subscribe(HomekitEventType eventType, UID publisherUID, UID subscriberUID,
            HomekitEventSubscriber subscriber) {
        HomekitEventSubscription subscription = new HomekitEventSubscription(eventType, publisherUID, subscriberUID,
                subscriber);
        if (subscriptions.add(subscription)) {
            publishEvent(new HomekitSubscriptionAddedEvent(subscription));
            logger.debug("{}Subscriber added for event type {} and source UID {}", LOG_SUBSCRIBER, eventType,
                    publisherUID);
        }
        return subscription;
    }

    public HomekitEventSubscription subscribe(HomekitEventType eventType, UID publisherUID, UID subscriberUID,
            HomekitEventSubscriber subscriber, Class<? extends HomekitEvent> expectedEventClass) {
        HomekitEventSubscription existingSubscription = findExistingSubscription(eventType, publisherUID, subscriber);
        return existingSubscription != null ? existingSubscription
                : createAndAddSubscription(eventType, publisherUID, subscriberUID, subscriber, expectedEventClass);
    }

    public HomekitEventSubscription subscribe(HomekitEventType eventType, UID publisherUID, UID subscriberUID,
            HomekitEventHandler handler, Class<? extends HomekitEvent> expectedEventClass) {
        HomekitEventSubscriber subscriberWrapper = new HomekitEventSubscriber() {
            @Override
            public void onEvent(HomekitEvent event) {
                handler.onEvent(event);
            }

            @Override
            public void onEventError(HomekitEvent event, Exception e) {
                handler.onError(event, e);
            }
        };

        return subscribe(eventType, publisherUID, subscriberUID, subscriberWrapper, expectedEventClass);
    }

    public HomekitEventSubscription subscribe(HomekitEventType eventType, UID publisherUID, UID subscriberUID,
            HomekitEventHandler handler) {
        HomekitEventSubscriber subscriberWrapper = new HomekitEventSubscriber() {
            @Override
            public void onEvent(HomekitEvent event) {
                handler.onEvent(event);
            }

            @Override
            public void onEventError(HomekitEvent event, Exception e) {
                handler.onError(event, e);
            }
        };

        return subscribe(eventType, publisherUID, subscriberUID, subscriberWrapper);
    }

    public List<HomekitEventSubscription> subscribe(Set<HomekitEventType> eventTypes, UID publisherUID,
            HomekitEventSubscriber subscriber) {
        return eventTypes.stream().map(eventType -> subscribe(eventType, publisherUID, subscriber))
                .collect(Collectors.toList());
    }

    public List<HomekitEventSubscription> subscribe(Set<HomekitEventType> eventTypes, UID publisherUID,
            UID subscriberUID, HomekitEventSubscriber subscriber) {
        return eventTypes.stream().map(eventType -> subscribe(eventType, publisherUID, subscriberUID, subscriber))
                .collect(Collectors.toList());
    }

    public List<HomekitEventSubscription> subscribe(Set<HomekitEventType> eventTypes, UID publisherUID,
            UID subscriberUID, HomekitEventSubscriber subscriber, Class<? extends HomekitEvent> expectedEventClass) {
        return eventTypes.stream()
                .map(eventType -> subscribe(eventType, publisherUID, subscriberUID, subscriber, expectedEventClass))
                .collect(Collectors.toList());
    }

    /**
     * Subscribes to events with a custom filter predicate.
     *
     * @param eventType the type of events to receive
     * @param publisherUID the UID of the publisher to receive events from
     * @param subscriber the subscriber that will receive the events
     * @param filter a predicate to filter events
     * @return the created subscription
     */
    public HomekitEventSubscription subscribe(HomekitEventType eventType, UID publisherUID,
            HomekitEventSubscriber subscriber, Predicate<HomekitEvent> filter) {
        HomekitEventSubscription subscription = new HomekitEventSubscription(eventType, publisherUID, subscriber,
                filter);
        if (subscriptions.add(subscription)) {
            publishEvent(new HomekitSubscriptionAddedEvent(subscription));
            logger.debug("{}Subscriber added for event type {} and source UID {} with filter", LOG_SUBSCRIBER,
                    eventType, publisherUID);
        }
        return subscription;
    }

    /**
     * Subscribes to events with a custom filter predicate and expected event class.
     *
     * @param eventType the type of events to receive
     * @param publisherUID the UID of the publisher to receive events from
     * @param subscriber the subscriber that will receive the events
     * @param expectedEventClass the expected class of events
     * @param filter a predicate to filter events
     * @return the created subscription
     */
    public HomekitEventSubscription subscribe(HomekitEventType eventType, UID publisherUID,
            HomekitEventSubscriber subscriber, Class<? extends HomekitEvent> expectedEventClass,
            Predicate<HomekitEvent> filter) {
        HomekitEventSubscription subscription = new HomekitEventSubscription(eventType, publisherUID, subscriber,
                expectedEventClass, filter);
        if (subscriptions.add(subscription)) {
            publishEvent(new HomekitSubscriptionAddedEvent(subscription));
            logger.debug("{}Subscriber added for event type {} and source UID {} with filter and expected class",
                    LOG_SUBSCRIBER, eventType, publisherUID);
        }
        return subscription;
    }

    /**
     * Subscribes to events with a custom filter predicate, subscriber UID, and expected event class.
     *
     * @param eventType the type of events to receive
     * @param publisherUID the UID of the publisher to receive events from
     * @param subscriberUID the UID of the subscriber
     * @param subscriber the subscriber that will receive the events
     * @param expectedEventClass the expected class of events
     * @param filter a predicate to filter events
     * @return the created subscription
     */
    public HomekitEventSubscription subscribe(HomekitEventType eventType, UID publisherUID, UID subscriberUID,
            HomekitEventSubscriber subscriber, Class<? extends HomekitEvent> expectedEventClass,
            Predicate<HomekitEvent> filter) {
        HomekitEventSubscription existingSubscription = findExistingSubscription(eventType, publisherUID, subscriber);
        return existingSubscription != null ? existingSubscription
                : createAndAddSubscription(eventType, publisherUID, subscriberUID, subscriber, expectedEventClass,
                        filter);
    }

    // Unsubscribe using the subscription object
    public void unsubscribe(HomekitEventSubscription subscription) {
        if (subscriptions.remove(subscription)) {
            publishEvent(new HomekitSubscriptionRemovedEvent(subscription));
            logger.debug("{}Subscriber removed for event type {} and source UID {}", LOG_SUBSCRIBER,
                    subscription.eventType, subscription.publisherUID);
        }
    }

    // Unsubscribe using eventType, publisherUID, and subscriber (for compatibility)
    public void unsubscribe(HomekitEventType eventType, UID publisherUID, HomekitEventSubscriber subscriber) {
        HomekitEventSubscription removedSubscription = findExistingSubscription(eventType, publisherUID, subscriber);
        if (removedSubscription != null && subscriptions.remove(removedSubscription)) {
            publishEvent(new HomekitSubscriptionRemovedEvent(removedSubscription));
            logger.debug("{}Subscriber removed for event type {} and source UID {}", LOG_SUBSCRIBER, eventType,
                    publisherUID);
        }
    }

    public List<HomekitEventSubscription> subscribe(Set<HomekitEventType> eventTypes, UID publisherUID,
            UID subscriberUID, HomekitEventHandler handler) {
        return eventTypes.stream().map(eventType -> subscribe(eventType, publisherUID, subscriberUID, handler))
                .collect(Collectors.toList());
    }

    public List<HomekitEventSubscription> subscribe(Set<HomekitEventType> eventTypes, UID publisherUID,
            UID subscriberUID, HomekitEventHandler handler, Class<? extends HomekitEvent> expectedEventClass) {
        return eventTypes.stream()
                .map(eventType -> subscribe(eventType, publisherUID, subscriberUID, handler, expectedEventClass))
                .collect(Collectors.toList());
    }

    /**
     * Handles errors that occur during event publishing, including retries and final error notification.
     *
     * @param event the event being published
     * @param subscriber the subscriber that failed (may be null)
     * @param retryCount the current retry attempt
     * @param e the exception that occurred
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

    private void handleEventError(HomekitEvent event, HomekitEventSubscriber subscriber, int retryCount, Exception e) {
        String errorMessage = "Event processing failed for subscriber " + subscriber + " on event " + event;

        // Always publish the failure event
        publishEvent(new HomekitSubscriptionFailureEvent(generateSubscriberUid(subscriber), event.getPublisherUID(),
                event.getType(), errorMessage, e));

        if (retryCount < MAX_RETRIES) {
            logger.warn("{} (retry {}/{})", errorMessage, retryCount + 1, MAX_RETRIES);
            // Schedule retry with exponential backoff
            long delay = RETRY_DELAY_MS * (1L << retryCount);
            eventExecutor.schedule(() -> {
                try {
                    publishEventToSubscribers(event, retryCount + 1, true);
                } catch (Exception retryException) {
                    logger.error("{}Retry failed for event {}: {}", LOG_ERROR, event.getPublisherUID(),
                            retryException.getMessage(), retryException);
                }
            }, delay, TimeUnit.MILLISECONDS);
        } else {
            logger.error("{} (max retries exceeded)", errorMessage, e);
            // Notify subscriber of final failure
            try {
                subscriber.onEventError(event, e);
            } catch (Exception notificationException) {
                logger.error("{}Error notifying subscriber of event failure: {}", LOG_ERROR,
                        notificationException.getMessage(), notificationException);
            }
            // Remove the subscription after max retries
            unsubscribe(event.getType(), event.getPublisherUID(), subscriber);
        }
    }

    /**
     * Shuts down the event manager, clearing the event queue and waiting for active events to complete.
     * Also shuts down the executor and clears all subscribers.
     */
    private void shutdown() {
        isRunning.set(false);

        // First, stop accepting new events
        eventQueue.clear();

        // Wait for active events with timeout
        long startTime = System.currentTimeMillis();
        while (activeEventCount.get() > 0 && (System.currentTimeMillis() - startTime) < SHUTDOWN_TIMEOUT_MS) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        if (activeEventCount.get() > 0) {
            logger.warn("{} events still active after shutdown timeout", activeEventCount.get());
        }

        // Shutdown executor
        eventExecutor.shutdown();
        try {
            if (!eventExecutor.awaitTermination(SHUTDOWN_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                List<Runnable> remainingTasks = eventExecutor.shutdownNow();
                logger.warn("{} tasks were still running after shutdown", remainingTasks.size());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            eventExecutor.shutdownNow();
        }

        // Final cleanup
        subscriptions.clear();
    }

    /**
     * Returns the total number of subscribers for a given event type.
     *
     * @param eventType the event type
     * @return the number of subscribers
     */
    public int getSubscriberCount(HomekitEventType eventType) {
        return (int) subscriptions.stream().filter(sub -> sub.eventType == eventType).count();
    }

    /**
     * Checks if there are any subscribers for a given event type.
     *
     * @param eventType the event type
     * @return true if there are subscribers, false otherwise
     */
    public boolean hasSubscribers(HomekitEventType eventType) {
        return subscriptions.stream().anyMatch(sub -> sub.eventType == eventType);
    }

    /**
     * Updates all subscriptions and events when an accessory is assigned to a server.
     * This method handles:
     * - Direct subscriptions to the old UID
     * - Wildcard subscriptions that match the old UID
     * - Events in the queue that reference the old UID
     * - Active publishers that use the old UID
     *
     * @param oldUID The temporary UID that is being replaced
     * @param newUID The new UID assigned by the server
     */
    public void updateAccessoryUID(HomekitAccessoryUID oldUID, HomekitAccessoryUID newUID) {
        // Update direct subscriptions
        subscriptions.stream().filter(sub -> sub.publisherUID.equals(oldUID)).forEach(sub -> {
            subscriptions.remove(sub);
            HomekitEventSubscription newSub = new HomekitEventSubscription(sub.eventType, newUID, sub.subscriberUID,
                    sub.subscriber, sub.expectedEventClass);
            subscriptions.add(newSub);
            logger.debug("{}Updated direct subscription from {} to {}", LOG_SUBSCRIBER, oldUID, newUID);
        });

        // Update wildcard subscriptions
        subscriptions.stream().filter(sub -> sub.publisherUID.equals(HomekitUID.WILDCARD_UID)
                || matchesPublisherPattern(sub.publisherUID, oldUID)).forEach(sub -> {
                    if (sub.subscriberUID.equals(oldUID)) {
                        subscriptions.remove(sub);
                        HomekitEventSubscription newSub = new HomekitEventSubscription(sub.eventType, sub.publisherUID,
                                newUID, sub.subscriber, sub.expectedEventClass);
                        subscriptions.add(newSub);
                        logger.debug("{}Updated wildcard subscription subscriber from {} to {}", LOG_SUBSCRIBER, oldUID,
                                newUID);
                    }
                });

        // Update events in the queue
        eventQueue.stream().filter(event -> event.getPublisherUID().equals(oldUID)).forEach(event -> {
            event.setPublisherUID(newUID);
            logger.debug("{}Updated event publisher UID from {} to {}", LOG_EVENT, oldUID, newUID);
        });

        // Update active publishers
        if (activePublishers.remove(oldUID)) {
            activePublishers.add(newUID);
            logger.debug("{}Updated active publisher from {} to {}", LOG_PUBLISHER, oldUID, newUID);
        }

        // Publish a notification event about the UID change
        HomekitAccessoryUID accessoryUID = newUID;
        HomekitAccessory accessory = accessoryRegistry.get(accessoryUID);
        if (accessory != null) {
            publishEvent(new HomekitAccessoryEvent(HomekitEventType.ACCESSORY_UID_CHANGED, accessory,
                    new HomekitAccessoryUID(oldUID.toString()), accessoryUID));
        } else {
            logger.warn("{}HomekitAccessory not found in registry for UID: {}", LOG_EVENT, newUID);
        }
    }

    /**
     * This method is kept for backward compatibility.
     */
    public void notifyUIDChange(HomekitAccessoryUID oldUID, HomekitAccessoryUID newUID) {
        updateAccessoryUID(oldUID, newUID);
    }

    /**
     * Returns a list of all subscriptions for a given subscriber UID.
     *
     * @param subscriberUid the subscriber UID to search for
     * @return list of matching subscriptions
     */
    public List<HomekitEventSubscription> getSubscriptionsBySubscriberUid(UID subscriberUid) {
        return subscriptions.stream().filter(sub -> sub.getSubscriberUID().equals(subscriberUid))
                .collect(Collectors.toList());
    }

    /**
     * Returns a list of all subscriptions for a given publisher UID.
     *
     * @param publisherUid the publisher UID to search for
     * @return list of matching subscriptions
     */
    public List<HomekitEventSubscription> getSubscriptionsByPublisherUid(UID publisherUid) {
        return subscriptions.stream().filter(sub -> sub.publisherUID.equals(publisherUid)).collect(Collectors.toList());
    }

    /**
     * Returns a list of all subscriptions for a given event type.
     *
     * @param eventType the event type to search for
     * @return list of matching subscriptions
     */
    public List<HomekitEventSubscription> getSubscriptionsByEventType(HomekitEventType eventType) {
        return subscriptions.stream().filter(sub -> sub.eventType == eventType).collect(Collectors.toList());
    }

    /**
     * Returns the total number of subscriptions for a given subscriber UID.
     *
     * @param subscriberUid the subscriber UID to count
     * @return number of subscriptions
     */
    public int getSubscriptionCountBySubscriberUid(UID subscriberUid) {
        return (int) subscriptions.stream().filter(sub -> sub.getSubscriberUID().equals(subscriberUid)).count();
    }

    /**
     * Returns the total number of subscriptions for a given publisher UID.
     *
     * @param publisherUid the publisher UID to count
     * @return number of subscriptions
     */
    public int getSubscriptionCountByPublisherUid(UID publisherUid) {
        return (int) subscriptions.stream().filter(sub -> sub.publisherUID.equals(publisherUid)).count();
    }

    /**
     * Bulk unsubscribes all subscriptions for a given subscriber UID.
     *
     * @param subscriberUid the subscriber UID to unsubscribe
     * @return number of subscriptions removed
     */
    public int unsubscribeAllBySubscriberUid(UID subscriberUid) {
        List<HomekitEventSubscription> toRemove = getSubscriptionsBySubscriberUid(subscriberUid);
        toRemove.forEach(this::unsubscribe);
        return toRemove.size();
    }

    /**
     * Bulk unsubscribes all subscriptions for a given publisher UID.
     *
     * @param publisherUid the publisher UID to unsubscribe
     * @return number of subscriptions removed
     */
    public int unsubscribeAllByPublisherUid(UID publisherUid) {
        List<HomekitEventSubscription> toRemove = getSubscriptionsByPublisherUid(publisherUid);
        toRemove.forEach(this::unsubscribe);
        return toRemove.size();
    }

    /**
     * Checks for orphaned subscriptions and logs diagnostic information.
     * A subscription is considered orphaned if:
     * - The publisher UID is not in the active publishers set
     * - The subscriber is no longer valid (e.g., garbage collected)
     */
    public void checkOrphanedSubscriptions() {
        int totalSubscriptions = subscriptions.size();
        int orphanedCount = 0;
        int idleCount = 0;
        long currentTime = System.currentTimeMillis();

        for (HomekitEventSubscription sub : subscriptions) {
            boolean isOrphaned = false;
            boolean isIdle = false;

            // Check if publisher is still active
            if (!activePublishers.contains(sub.publisherUID)) {
                isOrphaned = true;
                logger.warn("{}Orphaned subscription detected - Publisher {} is no longer active", LOG_WARN,
                        sub.publisherUID);
            }

            // Check if subscriber is still valid (using WeakReference)
            if (sub.subscriber == null) {
                isOrphaned = true;
                logger.warn("{}Orphaned subscription detected - Subscriber for publisher {} is no longer valid",
                        LOG_WARN, sub.publisherUID);
            }

            // Check for idle subscriptions (no events in last hour)
            if (currentTime - sub.getLastEventTime() > TimeUnit.HOURS.toMillis(1)) {
                isIdle = true;
                logger.debug("{}Idle subscription detected - No events for publisher {} in last hour", LOG_SUBSCRIBER,
                        sub.publisherUID);
            }

            if (isOrphaned) {
                orphanedCount++;
            }
            if (isIdle) {
                idleCount++;
            }
        }

        logger.info("{}Subscription Diagnostics - Total: {}, Orphaned: {}, Idle: {}", LOG_SUBSCRIBER,
                totalSubscriptions, orphanedCount, idleCount);
    }

    /**
     * Logs detailed statistics about subscriptions and publishers.
     */
    public void logSubscriptionStats() {
        logger.info("{}Total subscriptions: {}", LOG_SUBSCRIBER, subscriptions.size());
        logger.info("{}Active publishers: {}", LOG_PUBLISHER, activePublishers.size());

        // Group subscriptions by publisher
        Map<UID, Long> publisherCounts = subscriptions.stream()
                .collect(Collectors.groupingBy(sub -> sub.publisherUID, Collectors.counting()));

        publisherCounts.forEach((publisher, count) -> {
            logger.info("{}Publisher {} has {} subscriptions", LOG_PUBLISHER, publisher, count);
        });

        // Group subscriptions by event type
        Map<HomekitEventType, Long> eventTypeCounts = subscriptions.stream()
                .collect(Collectors.groupingBy(sub -> sub.eventType, Collectors.counting()));

        eventTypeCounts.forEach((type, count) -> {
            logger.info("{}Event type {} has {} subscriptions", LOG_SUBSCRIBER, type, count);
        });
    }

    private HomekitEventSubscription createAndAddSubscription(HomekitEventType eventType, UID publisherUID,
            UID subscriberUID, HomekitEventSubscriber subscriber, Class<? extends HomekitEvent> expectedEventClass,
            Predicate<HomekitEvent> filter) {
        HomekitEventSubscription subscription = new HomekitEventSubscription(eventType, publisherUID, subscriberUID,
                subscriber, expectedEventClass, filter);
        if (subscriptions.add(subscription)) {
            publishEvent(new HomekitSubscriptionAddedEvent(subscription));
            logger.debug("{}Subscriber added for event type {} and source UID {} with filter", LOG_SUBSCRIBER,
                    eventType, publisherUID);
        }
        return subscription;
    }

    /**
     * Returns metrics about event processing.
     * 
     * @return a map containing various event metrics
     */
    public Map<String, Integer> getEventMetrics() {
        Map<String, Integer> metrics = new HashMap<>();
        metrics.put("totalEventsPublished", totalEventsPublished.get());
        metrics.put("totalEventsProcessed", totalEventsProcessed.get());
        metrics.put("droppedEventsDueToLoops", droppedEventsDueToLoops.get());
        metrics.put("droppedEventsDueToHops", droppedEventsDueToHops.get());
        metrics.put("droppedEventsDueToCorrelation", droppedEventsDueToCorrelation.get());
        metrics.put("activeSubscriptions", subscriptions.size());
        metrics.put("activePublishers", activePublishers.size());
        metrics.put("queuedEvents", eventQueue.size());
        return metrics;
    }

    /**
     * Logs current event metrics.
     */
    public void logEventMetrics() {
        Map<String, Integer> metrics = getEventMetrics();
        metrics.forEach((name, value) -> logger.info("{}{}: {}", LOG_METRICS, name, value));
    }
}
