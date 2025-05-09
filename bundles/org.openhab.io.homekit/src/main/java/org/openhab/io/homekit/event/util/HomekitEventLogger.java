package org.openhab.io.homekit.event.util;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.event.HomekitEvent;
import org.openhab.io.homekit.api.event.HomekitEventSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton logger for Homekit events.
 * <p>
 * This class implements {@link HomekitEventSubscriber} and logs all received events and errors
 * using SLF4J. It is intended to be registered as a subscriber to all event types in the
 * {@link HomekitEventManager}.
 * </p>
 * <p>
 * Use {@link #getInstance()} to obtain the singleton instance.
 * </p>
 */
@NonNullByDefault
public class HomekitEventLogger implements HomekitEventSubscriber {
    private static final Logger logger = LoggerFactory.getLogger(HomekitEventLogger.class);
    private static final String LOG_PREFIX = "Homekit EventLogger: ";
    private static final String LOG_EVENT = LOG_PREFIX + "Event - ";
    private static final String LOG_WARN = LOG_PREFIX + "Warning - ";

    /**
     * The singleton instance of {@code HomekitEventLogger}.
     */
    private static final HomekitEventLogger INSTANCE = new HomekitEventLogger();

    /**
     * Private constructor to enforce singleton pattern.
     */
    private HomekitEventLogger() {
    }

    /**
     * Returns the singleton instance of {@code HomekitEventLogger}.
     *
     * @return the singleton instance
     */
    public static HomekitEventLogger getInstance() {
        return INSTANCE;
    }

    /**
     * Logs the received Homekit event at debug level.
     *
     * @param event the event to log
     */
    @Override
    public void onEvent(HomekitEvent event) {
        logEvent(event);
    }

    /**
     * Logs the details of the given Homekit event.
     *
     * @param event the event to log
     */
    private void logEvent(HomekitEvent event) {
        logger.debug("{}Received event: Type={}, Publisher={}, Timestamp={}, Data={}", LOG_EVENT, event.getType(),
                event.getPublisherUID(), event.getTimestamp(), event.toString());
    }

    /**
     * Logs an error that occurred while processing a Homekit event.
     *
     * @param event the event that caused the error
     * @param exception the exception thrown during event processing
     */
    @Override
    public void onEventError(HomekitEvent event, Exception exception) {
        logger.warn("{}Error processing event: {}, Exception: {}", LOG_WARN, event, exception.getMessage(), exception);
    }
}
