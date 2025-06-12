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

package org.openhab.io.homekit.event.model.service;

import java.util.Collections;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.UID;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristic;
import org.openhab.io.homekit.api.event.HomekitEventType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.event.core.AbstractHomekitEvent;
import org.openhab.io.homekit.event.core.HomekitEventMetadata;
import org.openhab.io.homekit.util.HomekitUID;

/**
 * Event class representing service-related events in the HomeKit integration.
 * This event is used to propagate changes and updates related to HomeKit services,
 * including service lifecycle events and characteristic changes within services.
 *
 * <p>
 * The class integrates with:
 * </p>
 * <ul>
 * <li>{@link org.openhab.io.homekit.api.event.HomekitEvent} for base event functionality</li>
 * <li>{@link org.openhab.io.homekit.api.event.HomekitEventType} for event type identification</li>
 * <li>{@link org.openhab.io.homekit.api.service.HomekitService} for service management</li>
 * <li>{@link org.openhab.io.homekit.api.characteristic.HomekitCharacteristic} for characteristic handling</li>
 * <li>{@link org.openhab.io.homekit.event.core.HomekitEventMetadata} for event metadata</li>
 * <li>{@link org.openhab.io.homekit.util.HomekitUID} for UID generation and management</li>
 * </ul>
 *
 * <p>
 * <b>Key Features:</b>
 * </p>
 * <ul>
 * <li>Service lifecycle management</li>
 * <li>Characteristic change tracking</li>
 * <li>Event metadata support</li>
 * <li>Optional component handling</li>
 * </ul>
 *
 * <p>
 * <b>Usage Patterns:</b>
 * </p>
 * <ul>
 * <li>Service state management</li>
 * <li>Characteristic value updates</li>
 * <li>Service configuration changes</li>
 * <li>System diagnostics and monitoring</li>
 * </ul>
 *
 * <p>
 * <b>Event Handling:</b>
 * </p>
 * <ul>
 * <li>Supports targeted event delivery</li>
 * <li>Maintains service relationships</li>
 * <li>Enables characteristic tracking</li>
 * <li>Supports wildcard subscribers</li>
 * </ul>
 *
 * @author Karel Goderis - Initial contribution
 * @since 3.x
 */
@NonNullByDefault
public class HomekitServiceEvent extends AbstractHomekitEvent {
    private final Optional<HomekitService> HomekitService;
    private final Optional<HomekitCharacteristic<?>> HomekitCharacteristic;

    /**
     * Creates a new service event with default publisher and subscriber.
     *
     * @param type the type of event
     * @param HomekitService the service associated with this event
     */
    @SuppressWarnings("null") // Suppresses null analysis warnings for nullable parameters with null checks in
                              // constructor
    public HomekitServiceEvent(HomekitEventType type, @Nullable HomekitService HomekitService) {
        super(type, HomekitService != null ? (UID) HomekitService.getUID() : (UID) new HomekitUID("HomekitService"),
                HomekitUID.WILDCARD_UID,
                new HomekitEventMetadata(
                        HomekitService != null ? (UID) HomekitService.getUID() : (UID) new HomekitUID("HomekitService"),
                        null, null, Collections.emptySet()));
        this.HomekitService = Optional.ofNullable(HomekitService);
        this.HomekitCharacteristic = Optional.empty();
    }

    /**
     * Creates a new service event with default subscriber UID.
     *
     * @param type the type of event
     * @param HomekitService the service associated with the event, if any
     * @param HomekitCharacteristic the characteristic associated with the event, if any
     */
    @SuppressWarnings("null") // Suppresses null analysis warnings for nullable parameters with null checks in
                              // constructor
    public HomekitServiceEvent(HomekitEventType type, @Nullable HomekitService HomekitService,
            @Nullable HomekitCharacteristic<?> HomekitCharacteristic) {
        super(type, HomekitService != null ? (UID) HomekitService.getUID() : (UID) new HomekitUID("HomekitService"),
                HomekitUID.WILDCARD_UID,
                new HomekitEventMetadata(
                        HomekitService != null ? (UID) HomekitService.getUID() : (UID) new HomekitUID("HomekitService"),
                        null, null, Collections.emptySet()));
        this.HomekitService = Optional.ofNullable(HomekitService);
        this.HomekitCharacteristic = Optional.ofNullable(HomekitCharacteristic);
    }

    /**
     * Creates a new service event with explicit subscriber UID.
     *
     * @param type the type of event
     * @param HomekitService the service associated with the event, if any
     * @param subscriberUID the UID of the subscriber that will receive the event
     * @param HomekitCharacteristic the characteristic associated with the event, if any
     */
    @SuppressWarnings("null") // Suppresses null analysis warnings for nullable parameters with null checks in
                              // constructor
    public HomekitServiceEvent(HomekitEventType type, @Nullable HomekitService HomekitService, UID subscriberUID,
            @Nullable HomekitCharacteristic<?> HomekitCharacteristic) {
        super(type, HomekitService != null ? (UID) HomekitService.getUID() : (UID) new HomekitUID("HomekitService"),
                subscriberUID,
                new HomekitEventMetadata(
                        HomekitService != null ? (UID) HomekitService.getUID() : (UID) new HomekitUID("HomekitService"),
                        null, null, Collections.emptySet()));
        this.HomekitService = Optional.ofNullable(HomekitService);
        this.HomekitCharacteristic = Optional.ofNullable(HomekitCharacteristic);
    }

    /**
     * Returns the service associated with this event.
     *
     * @return an Optional containing the service, or empty if not available
     */
    public Optional<HomekitService> getService() {
        return HomekitService;
    }

    /**
     * Returns the characteristic associated with this event.
     *
     * @return an Optional containing the characteristic, or empty if not available
     */
    public Optional<HomekitCharacteristic<?>> getCharacteristic() {
        return HomekitCharacteristic;
    }

    @Override
    public String toString() {
        return "HomekitServiceEvent{" + "type=" + getType() + ", publisherUID=" + getPublisherUID() + ", timestamp="
                + getTimestamp() + ", HomekitService=" + HomekitService + ", HomekitCharacteristic="
                + HomekitCharacteristic + '}';
    }
}
