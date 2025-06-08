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

package org.openhab.io.homekit.api.event;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Interface for subscribing to and handling HomeKit events in the system.
 * <p>
 * This interface defines the contract for components that need to receive and process
 * HomeKit events. It provides methods for handling both successful event delivery and
 * error conditions that may occur during event processing.
 * </p>
 * <p>
 * Key implementation details:
 * <ul>
 * <li>Event reception and processing</li>
 * <li>Error handling and recovery</li>
 * <li>Thread-safe event handling</li>
 * <li>Asynchronous event processing support</li>
 * </ul>
 * </p>
 * <p>
 * The interface integrates with:
 * <ul>
 * <li>{@link org.openhab.io.homekit.api.event.HomekitEvent} for event handling</li>
 * <li>{@link org.openhab.io.homekit.api.event.HomekitEventPublisher} for event reception</li>
 * </ul>
 * </p>
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0.0
 */
@NonNullByDefault
public interface HomekitEventSubscriber {
    /**
     * Handles a received HomeKit event.
     * This method is called when an event matching the subscriber's criteria is published.
     * Implementations should process the event according to their specific requirements.
     *
     * @param event the event to handle
     * @since 1.0.0
     */
    void onEvent(HomekitEvent event);

    /**
     * Handles errors that occur during event processing.
     * This method is called when an exception occurs while processing an event.
     * Implementations should handle the error appropriately, which may include:
     * - Logging the error
     * - Attempting recovery
     * - Notifying administrators
     * - Cleaning up resources
     *
     * @param event the event that caused the error
     * @param e the exception that occurred
     * @since 1.0.0
     */
    void onEventError(HomekitEvent event, Exception e);
}
