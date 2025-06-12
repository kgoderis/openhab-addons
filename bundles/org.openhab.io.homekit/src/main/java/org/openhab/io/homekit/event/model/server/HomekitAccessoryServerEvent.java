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

package org.openhab.io.homekit.event.model.server;

import java.util.Collections;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.UID;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristic;
import org.openhab.io.homekit.api.event.HomekitEventType;
import org.openhab.io.homekit.api.server.HomekitAccessoryServer;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.event.core.AbstractHomekitEvent;
import org.openhab.io.homekit.event.core.HomekitEventMetadata;
import org.openhab.io.homekit.util.HomekitUID;

/**
 * Event class representing server-related events in the HomeKit integration.
 * This event is used to propagate changes and updates related to HomeKit accessory servers,
 * including server lifecycle events, accessory registration, service updates, and characteristic changes.
 *
 * <p>
 * The class integrates with:
 * </p>
 * <ul>
 * <li>{@link org.openhab.io.homekit.api.event.HomekitEvent} for base event functionality</li>
 * <li>{@link org.openhab.io.homekit.api.event.HomekitEventType} for event type identification</li>
 * <li>{@link org.openhab.io.homekit.api.server.HomekitAccessoryServer} for server management</li>
 * <li>{@link org.openhab.io.homekit.api.accessory.HomekitAccessory} for accessory handling</li>
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
 * <li>Server lifecycle management</li>
 * <li>Accessory registration tracking</li>
 * <li>Service update propagation</li>
 * <li>Characteristic change handling</li>
 * <li>Event metadata support</li>
 * </ul>
 *
 * <p>
 * <b>Usage Patterns:</b>
 * </p>
 * <ul>
 * <li>Server state management</li>
 * <li>Accessory lifecycle tracking</li>
 * <li>Service configuration updates</li>
 * <li>Characteristic value changes</li>
 * <li>System diagnostics and monitoring</li>
 * </ul>
 *
 * <p>
 * <b>Event Propagation:</b>
 * </p>
 * <ul>
 * <li>Supports hierarchical event routing</li>
 * <li>Maintains component relationships</li>
 * <li>Enables targeted event delivery</li>
 * <li>Supports wildcard subscribers</li>
 * </ul>
 *
 * @author Karel Goderis - Initial contribution
 * @since 3.x
 */
@NonNullByDefault
public class HomekitAccessoryServerEvent extends AbstractHomekitEvent {
    private final Optional<HomekitAccessoryServer> server;
    private final Optional<HomekitAccessory> accessory;
    private final Optional<HomekitService> service;
    private final Optional<HomekitCharacteristic<?>> characteristic;

    /**
     * Creates a new server event with default metadata.
     *
     * @param type the type of event
     * @param server the server associated with the event
     * @param accessory the accessory associated with the event, if any
     * @param service the service associated with the event, if any
     * @param characteristic the characteristic associated with the event, if any
     */
    @SuppressWarnings("null") // Suppresses null analysis warnings for nullable parameters and type casting operations
    public HomekitAccessoryServerEvent(HomekitEventType type, HomekitAccessoryServer server,
            @Nullable HomekitAccessory accessory, @Nullable HomekitService service,
            @Nullable HomekitCharacteristic<?> characteristic) {
        super(type, server != null ? (UID) server.getUID() : (UID) new HomekitUID("server"), WILDCARD_UID,
                new HomekitEventMetadata(server != null ? (UID) server.getUID() : (UID) new HomekitUID("server"), null,
                        null, Collections.emptySet()));
        this.server = Optional.ofNullable(server);
        this.accessory = Optional.ofNullable(accessory);
        this.service = Optional.ofNullable(service);
        this.characteristic = Optional.ofNullable(characteristic);
    }

    /**
     * Creates a new server event with custom metadata.
     *
     * @param type the type of event
     * @param server the server associated with the event
     * @param accessory the accessory associated with the event, if any
     * @param service the service associated with the event, if any
     * @param characteristic the characteristic associated with the event, if any
     * @param metadata additional metadata for the event
     */
    @SuppressWarnings("null") // Suppresses null analysis warnings for nullable parameters and type casting operations
    public HomekitAccessoryServerEvent(HomekitEventType type, HomekitAccessoryServer server,
            @Nullable HomekitAccessory accessory, @Nullable HomekitService service,
            @Nullable HomekitCharacteristic<?> characteristic, HomekitEventMetadata metadata) {
        super(type, server != null ? (UID) server.getUID() : (UID) new HomekitUID("server"), WILDCARD_UID, metadata);
        this.server = Optional.ofNullable(server);
        this.accessory = Optional.ofNullable(accessory);
        this.service = Optional.ofNullable(service);
        this.characteristic = Optional.ofNullable(characteristic);
    }

    /**
     * Returns the server associated with this event.
     *
     * @return an Optional containing the server, or empty if not available
     */
    public Optional<HomekitAccessoryServer> getServer() {
        return server;
    }

    /**
     * Returns the accessory associated with this event.
     *
     * @return an Optional containing the accessory, or empty if not available
     */
    public Optional<HomekitAccessory> getAccessory() {
        return accessory;
    }

    /**
     * Returns the service associated with this event.
     *
     * @return an Optional containing the service, or empty if not available
     */
    public Optional<HomekitService> getService() {
        return service;
    }

    /**
     * Returns the characteristic associated with this event.
     *
     * @return an Optional containing the characteristic, or empty if not available
     */
    public Optional<HomekitCharacteristic<?>> getCharacteristic() {
        return characteristic;
    }

    @Override
    public String toString() {
        return "HomekitAccessoryServerEvent{" + "type=" + getType() + ", publisherUID=" + getPublisherUID()
                + ", timestamp=" + getTimestamp() + ", server=" + server + ", accessory=" + accessory + ", service="
                + service + ", characteristic=" + characteristic + '}';
    }
}
