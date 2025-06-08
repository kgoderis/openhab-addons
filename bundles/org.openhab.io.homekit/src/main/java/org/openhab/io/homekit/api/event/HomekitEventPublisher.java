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
import org.openhab.core.thing.UID;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * Interface for publishing HomeKit events in the system.
 * <p>
 * This interface defines the contract for components that need to publish HomeKit events.
 * It provides methods to identify the event source and access the event management system.
 * The interface includes a default implementation for publishing events that automatically
 * sets the publisher UID and delegates to the event manager.
 * </p>
 * <p>
 * Key implementation details:
 * <ul>
 * <li>Source identification through UID</li>
 * <li>Event manager integration</li>
 * <li>Automatic publisher UID assignment</li>
 * <li>Thread-safe event publishing</li>
 * </ul>
 * </p>
 * <p>
 * The interface integrates with:
 * <ul>
 * <li>{@link org.openhab.core.thing.UID} for source identification</li>
 * <li>{@link org.openhab.io.homekit.event.manager.HomekitEventManager} for event management</li>
 * <li>{@link org.openhab.io.homekit.api.event.HomekitEvent} for event handling</li>
 * </ul>
 * </p>
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0.0
 */
@NonNullByDefault
public interface HomekitEventPublisher {
    /**
     * Gets the unique identifier of the event publisher.
     * This UID is used to identify the source of published events.
     *
     * @return the unique identifier of the publisher
     * @since 1.0.0
     */
    UID getSourceUID();

    /**
     * Gets the event manager instance used for publishing events.
     * The event manager handles the distribution and processing of events.
     *
     * @return the event manager instance
     * @since 1.0.0
     */
    HomekitEventManager getEventManager();

    /**
     * Publishes a HomeKit event to the system.
     * This default implementation:
     * 1. Sets the publisher UID on the event
     * 2. Delegates the event to the event manager for processing
     *
     * @param event the event to publish
     * @since 1.0.0
     */
    default void publishEvent(HomekitEvent event) {
        event.setPublisherUID(getSourceUID());
        getEventManager().publishEvent(event);
    }
}
