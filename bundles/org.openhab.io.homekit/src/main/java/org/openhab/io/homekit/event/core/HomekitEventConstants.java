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

package org.openhab.io.homekit.event.core;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.event.HomekitEvent;

/**
 * Constants used throughout the HomeKit event system in the OpenHAB HomeKit integration.
 *
 * <p>
 * This class defines system-wide constants used for event handling, including thread names,
 * timeouts, and standard event property names. These constants ensure consistency across
 * the event system and provide a centralized location for configuration values.
 * </p>
 *
 * <p>
 * The class integrates with:
 * </p>
 * <ul>
 * <li>{@link HomekitEvent} for event property definitions</li>
 * <li>{@link HomekitEventDispatcher} for thread management</li>
 * <li>{@link HomekitEventMetadata} for event property handling</li>
 * </ul>
 *
 * <p>
 * <b>Key Constants:</b>
 * </p>
 * <ul>
 * <li>Thread Management:
 * <ul>
 * <li>EVENT_THREAD_NAME: Name of the event dispatch thread</li>
 * <li>SHUTDOWN_TIMEOUT_SECONDS: Graceful shutdown timeout</li>
 * </ul>
 * </li>
 * <li>Event Properties:
 * <ul>
 * <li>POWER: Power state property</li>
 * <li>BRIGHTNESS: Brightness level property</li>
 * <li>TEMPERATURE: Temperature value property</li>
 * <li>HUMIDITY: Humidity level property</li>
 * </ul>
 * </li>
 * </ul>
 *
 * <p>
 * <b>Usage Guidelines:</b>
 * </p>
 * <ul>
 * <li>Use these constants instead of string literals for consistency</li>
 * <li>Add new constants for any new standard event properties</li>
 * <li>Maintain thread-related constants for system configuration</li>
 * </ul>
 *
 * @author Karel Goderis - Initial contribution
 * @since 3.x
 */
@NonNullByDefault
public class HomekitEventConstants {
    public static final String EVENT_THREAD_NAME = "homekit-event-dispatcher";
    public static final int SHUTDOWN_TIMEOUT_SECONDS = 5;

    public static final String POWER = "power";
    public static final String BRIGHTNESS = "brightness";
    public static final String TEMPERATURE = "temperature";
    public static final String HUMIDITY = "humidity";
}
