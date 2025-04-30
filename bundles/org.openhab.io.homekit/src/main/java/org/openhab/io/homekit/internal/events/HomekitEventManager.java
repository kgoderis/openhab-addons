package org.openhab.io.homekit.internal.events;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.ThreadPoolManager;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages HomeKit event publishing and subscription for the OpenHAB HomeKit integration.
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

    private static final Logger logger = LoggerFactory.getLogger(HomekitEventManager.class);
    private static final String LOG_PREFIX = "HomeKit EventManager: ";
    private static final String LOG_EVENT = LOG_PREFIX + "Event - ";
    private static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    private static final String LOG_WARN = LOG_PREFIX + "Warning - ";
    private static final String LOG_SUBSCRIBER = LOG_PREFIX + "Subscriber - ";
    private static final String LOG_QUEUE = LOG_PREFIX + "Queue - ";
    private static final String LOG_INIT = LOG_PREFIX + "Init - ";

    private final Set<HomekitEventSubscription> subscriptions = ConcurrentHashMap.newKeySet();
    private final PriorityBlockingQueue<HomekitEvent> eventQueue = new PriorityBlockingQueue<>(MAX_QUEUE_SIZE,
            (e1, e2) -> Long.compare(e1.getTimestamp(), e2.getTimestamp()));
    private final ScheduledExecutorService eventExecutor;
    private final AtomicBoolean isRunning = new AtomicBoolean(true);
    private final AtomicInteger activeEventCount = new AtomicInteger(0);
    private final HomekitEventLogger eventLogger;

    @FunctionalInterface
    public interface HomekitEventHandler {
        void handle(HomekitEvent event);
    }

    /**
     * Constructs and activates the HomeKit Event Manager.
     * Initializes the event processor.
     */
    @Activate
    public HomekitEventManager() {
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
        shutdown();
        logger.debug("{}Deactivated", LOG_INIT);
    }

    // Register the logger as a regular subscription for all event types and all sources
    private void registerEventLogger() {
        for (HomekitEventType type : HomekitEventType.values()) {
            subscribe(type, "*", eventLogger);
        }
        logger.debug("{}Event logger registered for all event types", LOG_INIT);
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
                logger.debug("{}Event queued: type={}, sourceUid={}, timestamp={}", LOG_EVENT, event.getType(),
                        event.getSourceUid(), event.getTimestamp());
            } else {
                logger.warn("{}Failed to queue event after timeout, event discarded", LOG_QUEUE);
            }
        } catch (Exception e) {
            logger.error("{}Error queuing event: {}", LOG_ERROR, e.getMessage(), e);
        }
    }


    private CompletableFuture<Void> publishEventToSubscribers(HomekitEvent event, int retryCount, boolean retry) {
        List<HomekitEventSubscription> matchingSubs = new ArrayList<>();
        for (HomekitEventSubscription sub : subscriptions) {
            if (sub.eventType == event.getType() &&
                (sub.sourceUid.equals("*") || sub.sourceUid.equals(event.getSourceUid()))) {
                matchingSubs.add(sub);
            }
        }
        List<CompletableFuture<Void>> futures=  createSubscriberFutures(event, matchingSubs, retryCount, retry);

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .orTimeout(EVENT_TIMEOUT_MS, TimeUnit.MILLISECONDS).exceptionally(throwable -> {
                    logger.error("{}Error during synchronized event publishing: {}", LOG_ERROR, throwable.getMessage(),
                            throwable);
                    return null;
                });
    }


    private List<CompletableFuture<Void>> createSubscriberFutures(HomekitEvent event, List<HomekitEventSubscription> subs, int retryCount, boolean retry) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (HomekitEventSubscription sub : subs) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    sub.subscriber.onEvent(event);
                } catch (Exception e) {
                    handleEventError(event, sub.subscriber, retryCount, e);
                }
            }, eventExecutor).orTimeout(EVENT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .exceptionally(e -> {
                logger.error("{}Event timeout or error for source UID {}: {}", LOG_ERROR, event.getSourceUid(), e.getMessage());
                handleEventError(event, sub.subscriber, MAX_RETRIES, new TimeoutException(e.getMessage()));
                return null;
            });
            futures.add(future);
        }
        return futures;
    }

    /**
     * Subscribes a {@link HomekitEventSubscriber} to a specific event type and source UID.
     *
     * @param eventType the event type to subscribe to
     * @param sourceUid the source UID to subscribe to (or "*" for all sources)
     * @param subscriber the subscriber to notify
     */
    public HomekitEventSubscription subscribe(HomekitEventType eventType, String sourceUid, HomekitEventSubscriber subscriber) {
        HomekitEventSubscription subscription = new HomekitEventSubscription(eventType, sourceUid, subscriber);
        subscriptions.add(subscription);
        logger.debug("{}Subscriber added for event type {} and source UID {}", LOG_SUBSCRIBER, eventType, sourceUid);
        return subscription;
    }

    public HomekitEventSubscription subscribe(HomekitEventType eventType, String sourceUid, HomekitEventHandler handler) {

        HomekitEventSubscriber subscriber = new HomekitEventSubscriber() {
            @Override
            public void onEvent(HomekitEvent event) {
                handler.handle(event);
            }

            @Override
            public void onEventError(HomekitEvent event, Exception e) {
                // Optionally handle errors here or provide another lambda for errors
            }
        };

        return        subscribe(eventType, sourceUid, subscriber);

    }

      // Unsubscribe using the subscription object
      public void unsubscribe(HomekitEventSubscription subscription) {
        subscriptions.remove(subscription);
        logger.debug("{}Subscriber removed for event type {} and source UID {}", LOG_SUBSCRIBER, subscription.eventType, subscription.sourceUid);
    }

    // Unsubscribe using eventType, sourceUid, and subscriber (for compatibility)
    public void unsubscribe(HomekitEventType eventType, String sourceUid, HomekitEventSubscriber subscriber) {
        subscriptions.removeIf(sub ->
            sub.eventType == eventType &&
            sub.sourceUid.equals(sourceUid) &&
            sub.subscriber == subscriber
        );
        logger.debug("{}Subscriber removed for event type {} and source UID {}", LOG_SUBSCRIBER, eventType, sourceUid);
    }

    /**
     * Handles errors that occur during event publishing, including retries and final error notification.
     *
     * @param event the event being published
     * @param subscriber the subscriber that failed (may be null)
     * @param retryCount the current retry attempt
     * @param e the exception that occurred
     */
    private void handleEventError(HomekitEvent event, @Nullable HomekitEventSubscriber subscriber, int retryCount,
            Exception e) {
        if (e instanceof TimeoutException) {
            logger.warn("{}Event timeout for source UID {} (attempt {}/{}): {}", LOG_WARN, event.getSourceUid(),
                    retryCount + 1, MAX_RETRIES, e.getMessage());
        } else {
            logger.error("{}Error publishing event for source UID {} (attempt {}/{}): {}", LOG_ERROR,
                    event.getSourceUid(), retryCount + 1, MAX_RETRIES, e != null ? e.getMessage() : "", e);
        }

        if (retryCount < MAX_RETRIES) {
            long delay = RETRY_DELAY_MS * (1L << retryCount);
            eventExecutor.schedule(() -> {
                try {
                    publishEventToSubscribers(event, retryCount + 1, true);
                } catch (Exception retryException) {
                    logger.error("{}Retry failed for event {}: {}", LOG_ERROR, event.getSourceUid(),
                            retryException.getMessage(), retryException);
                }
            }, delay, TimeUnit.MILLISECONDS);
        } else {
            // Notify subscribers of final failure
            if (subscriber != null) {
                try {
                    subscriber.onEventError(event, e);
                } catch (Exception notificationException) {
                    logger.error("{}Error notifying subscriber of event failure: {}", LOG_ERROR,
                            notificationException.getMessage(), notificationException);
                }
            }
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
        return (int) subscriptions.stream()
                .filter(sub -> sub.eventType == eventType)
                .count();
    }

    /**
     * Checks if there are any subscribers for a given event type.
     *
     * @param eventType the event type
     * @return true if there are subscribers, false otherwise
     */
    public boolean hasSubscribers(HomekitEventType eventType) {
        return subscriptions.stream()
                .anyMatch(sub -> sub.eventType == eventType);
    }


}
