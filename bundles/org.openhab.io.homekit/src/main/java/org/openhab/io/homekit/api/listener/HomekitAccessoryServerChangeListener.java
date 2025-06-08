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

package org.openhab.io.homekit.api.listener;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.event.model.server.HomekitAccessoryServerEvent;

/**
 * Interface for listening to changes in HomeKit accessory servers.
 *
 * This interface defines the contract for components that need to be notified of changes
 * in HomeKit accessory servers, including server events and state changes.
 *
 * The interface provides:
 * - Server event notifications
 * - State change tracking
 * - Event handling
 *
 * Key implementation details:
 * - Event-based notification system
 * - Thread-safe event handling
 * - Server state tracking
 * - Event propagation
 *
 * The interface integrates with:
 * - {@link org.openhab.io.homekit.event.model.server.HomekitAccessoryServerEvent} for server events
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0.0
 */
@NonNullByDefault
public interface HomekitAccessoryServerChangeListener {
    /**
     * Called when a server event occurs.
     * This method is invoked when a HomeKit accessory server generates an event.
     *
     * @param event The server event that occurred
     * @since 1.0.0
     */
    void onAccessoryServerEvent(HomekitAccessoryServerEvent event);
}
