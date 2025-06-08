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
import org.openhab.io.homekit.event.model.service.HomekitServiceEvent;

/**
 * Listener interface for HomeKit service events.
 * <p>
 * This interface defines the contract for components that need to be notified of changes
 * in HomeKit services, including service events and state changes.
 * </p>
 * <p>
 * The interface provides:
 * <ul>
 * <li>Service event notifications</li>
 * <li>State change tracking</li>
 * <li>Event handling</li>
 * </ul>
 * </p>
 * <p>
 * Key implementation details:
 * <ul>
 * <li>Event-based notification system</li>
 * <li>Thread-safe event handling</li>
 * <li>Service state tracking</li>
 * <li>Event propagation</li>
 * </ul>
 * </p>
 * <p>
 * The interface integrates with:
 * <ul>
 * <li>{@link org.openhab.io.homekit.event.model.service.HomekitServiceEvent} for service events</li>
 * </ul>
 * </p>
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0.0
 */
@NonNullByDefault
public interface HomekitServiceChangeListener {
    void onServiceEvent(HomekitServiceEvent serviceEvent);
}
