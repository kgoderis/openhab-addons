/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.openhab.io.homekit.event.util;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.event.HomekitEvent;
import org.openhab.io.homekit.api.event.HomekitEventSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton logger implementation for HomeKit events in the OpenHAB system.
 * This class implements {@link HomekitEventSubscriber} to provide centralized logging
 * of all HomeKit events and their processing status.
 *
 * The class integrates with:
 * - {@link HomekitEventSubscriber} for event subscription functionality
 * - {@link HomekitEvent} for event data handling
 * - {@link org.slf4j.Logger SLF4J} for logging implementation
 *
 * Key Features:
 * - Centralized event logging
 * - Error tracking and reporting
 * - Thread-safe singleton implementation
 * - Consistent log formatting
 * - Debug and warning level logging
 *
 * Logging Patterns:
 * - Event reception: Logs event type, publisher, timestamp, and data
 * - Error handling: Logs event details and exception information
 * - Warning conditions: Logs potential issues and processing errors
 *
 * Usage Guidelines:
 * - Access the logger using {@link #getInstance()}
 * - Subscribe to events using the {@link HomekitEventSubscriber} interface
 * - Monitor debug logs for event flow
 * - Check warning logs for error conditions
 * - Use consistent log prefixes for filtering
 *
 * Error Handling:
 * - Captures and logs all event processing exceptions
 * - Maintains event context in error logs
 * - Provides detailed error information for debugging
 * - Preserves stack traces for error analysis
 *
 * @author Karel Goderis - Initial contribution
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
     * Initializes the logger instance.
     */
    private HomekitEventLogger() {
    }

    /**
     * Returns the singleton instance of {@code HomekitEventLogger}.
     * This method ensures that only one instance of the logger exists
     * throughout the application.
     *
     * @return the singleton instance
     */
    public static HomekitEventLogger getInstance() {
        return INSTANCE;
    }

    /**
     * Logs the received HomeKit event at debug level.
     * This method is called by the event system when a new event is received.
     *
     * @param event the event to log
     */
    @Override
    public void onEvent(HomekitEvent event) {
        logEvent(event);
    }

    /**
     * Logs the details of the given HomeKit event.
     * Includes event type, publisher, timestamp, and event data.
     *
     * @param event the event to log
     */
    private void logEvent(HomekitEvent event) {
        logger.debug("{}Received event: Type={}, Publisher={}, Timestamp={}, Data={}", LOG_EVENT, event.getType(),
                event.getPublisherUID(), event.getTimestamp(), event.toString());
    }

    /**
     * Logs an error that occurred while processing a HomeKit event.
     * This method is called by the event system when an error occurs
     * during event processing.
     *
     * @param event the event that caused the error
     * @param exception the exception thrown during event processing
     */
    @Override
    public void onEventError(HomekitEvent event, Exception exception) {
        logger.warn("{}Error processing event: {}, Exception: {}", LOG_WARN, event, exception.getMessage(), exception);
    }
}
