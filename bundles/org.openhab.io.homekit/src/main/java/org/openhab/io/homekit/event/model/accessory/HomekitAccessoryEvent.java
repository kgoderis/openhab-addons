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

package org.openhab.io.homekit.event.model.accessory;

import java.util.Collections;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.UID;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristic;
import org.openhab.io.homekit.api.event.HomekitEventType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.api.uid.HomekitAccessoryUID;
import org.openhab.io.homekit.event.core.AbstractHomekitEvent;
import org.openhab.io.homekit.event.core.HomekitEventMetadata;
import org.openhab.io.homekit.util.HomekitUID;

/**
 * Event class representing accessory-related events in the HomeKit integration.
 * This event is used to propagate changes and updates related to HomeKit accessories,
 * including accessory lifecycle events, service updates, characteristic changes, and UID modifications.
 *
 * <p>
 * The class integrates with:
 * </p>
 * <ul>
 * <li>{@link org.openhab.io.homekit.api.event.HomekitEvent} for base event functionality</li>
 * <li>{@link org.openhab.io.homekit.api.event.HomekitEventType} for event type identification</li>
 * <li>{@link org.openhab.io.homekit.api.accessory.HomekitAccessory} for accessory management</li>
 * <li>{@link org.openhab.io.homekit.api.service.HomekitService} for service handling</li>
 * <li>{@link org.openhab.io.homekit.api.characteristic.HomekitCharacteristic} for characteristic handling</li>
 * <li>{@link org.openhab.io.homekit.api.uid.HomekitAccessoryUID} for accessory identification</li>
 * <li>{@link org.openhab.io.homekit.event.core.HomekitEventMetadata} for event metadata</li>
 * <li>{@link org.openhab.io.homekit.util.HomekitUID} for UID generation and management</li>
 * </ul>
 *
 * <p>
 * <b>Key Features:</b>
 * </p>
 * <ul>
 * <li>Accessory lifecycle management</li>
 * <li>Service and characteristic tracking</li>
 * <li>UID change handling</li>
 * <li>Event metadata support</li>
 * <li>Optional component handling</li>
 * </ul>
 *
 * <p>
 * <b>Usage Patterns:</b>
 * </p>
 * <ul>
 * <li>Accessory state management</li>
 * <li>Service configuration updates</li>
 * <li>Characteristic value changes</li>
 * <li>UID modification tracking</li>
 * <li>System diagnostics and monitoring</li>
 * </ul>
 *
 * <p>
 * <b>Event Handling:</b>
 * </p>
 * <ul>
 * <li>Supports hierarchical event routing</li>
 * <li>Maintains component relationships</li>
 * <li>Enables targeted event delivery</li>
 * <li>Supports wildcard subscribers</li>
 * </ul>
 *
 * <p>
 * <b>UID Management:</b>
 * </p>
 * <ul>
 * <li>Tracks UID changes</li>
 * <li>Maintains UID history</li>
 * <li>Supports UID validation</li>
 * <li>Enables UID correlation</li>
 * </ul>
 *
 * @author Karel Goderis - Initial contribution
 * @since 3.x
 */
@NonNullByDefault
public class HomekitAccessoryEvent extends AbstractHomekitEvent {
    private final Optional<HomekitAccessory> accessory;
    private final Optional<HomekitService> service;
    private final Optional<HomekitCharacteristic<?>> characteristic;
    private final Optional<HomekitAccessoryUID> oldUid;
    private final Optional<HomekitAccessoryUID> newUid;

    /**
     * Creates a new accessory event with default metadata.
     *
     * @param type the type of event
     * @param accessory the accessory associated with the event, if any
     * @param service the service associated with the event, if any
     * @param characteristic the characteristic associated with the event, if any
     */
    @SuppressWarnings("null") // Suppresses null analysis warnings for nullable parameters with null checks in
                              // constructor
    public HomekitAccessoryEvent(HomekitEventType type, @Nullable HomekitAccessory accessory,
            @Nullable HomekitService service, @Nullable HomekitCharacteristic<?> characteristic) {
        super(type, accessory != null ? (UID) accessory.getUID() : (UID) new HomekitUID("accessory"),
                HomekitUID.WILDCARD_UID,
                new HomekitEventMetadata(accessory != null ? (UID) accessory.getUID() : (UID) HomekitUID.WILDCARD_UID,
                        (UID) null, (UID) null, Collections.emptySet()));
        this.accessory = Optional.ofNullable(accessory);
        this.service = Optional.ofNullable(service);
        this.characteristic = Optional.ofNullable(characteristic);
        this.oldUid = Optional.empty();
        this.newUid = Optional.empty();
    }

    /**
     * Creates a new accessory event with custom metadata.
     *
     * @param type the type of event
     * @param accessory the accessory associated with the event, if any
     * @param service the service associated with the event, if any
     * @param characteristic the characteristic associated with the event, if any
     * @param metadata additional metadata for the event
     */
    @SuppressWarnings("null") // Suppresses null analysis warnings for nullable parameters with null checks in
                              // constructor
    public HomekitAccessoryEvent(HomekitEventType type, @Nullable HomekitAccessory accessory,
            @Nullable HomekitService service, @Nullable HomekitCharacteristic<?> characteristic,
            HomekitEventMetadata metadata) {
        super(type, accessory != null ? (UID) accessory.getUID() : (UID) new HomekitUID("accessory"),
                HomekitUID.WILDCARD_UID, metadata);
        this.accessory = Optional.ofNullable(accessory);
        this.service = Optional.ofNullable(service);
        this.characteristic = Optional.ofNullable(characteristic);
        this.oldUid = Optional.empty();
        this.newUid = Optional.empty();
    }

    /**
     * Creates a new accessory event for a UID change with default metadata.
     *
     * @param type the type of event
     * @param accessory the accessory associated with the event, if any
     * @param oldUid the previous UID of the accessory
     * @param newUid the new UID of the accessory
     */
    @SuppressWarnings("null") // Suppresses null analysis warnings for nullable parameters with null checks in
                              // constructor
    public HomekitAccessoryEvent(HomekitEventType type, @Nullable HomekitAccessory accessory,
            HomekitAccessoryUID oldUid, HomekitAccessoryUID newUid) {
        super(type, accessory != null ? (UID) accessory.getUID() : (UID) new HomekitUID("accessory"),
                HomekitUID.WILDCARD_UID,
                new HomekitEventMetadata(accessory != null ? (UID) accessory.getUID() : (UID) HomekitUID.WILDCARD_UID,
                        (UID) null, (UID) null, Collections.emptySet()));
        this.accessory = Optional.ofNullable(accessory);
        this.service = Optional.empty();
        this.characteristic = Optional.empty();
        this.oldUid = Optional.of(oldUid);
        this.newUid = Optional.of(newUid);
    }

    /**
     * Creates a new accessory event for a UID change with custom metadata.
     *
     * @param type the type of event
     * @param accessory the accessory associated with the event, if any
     * @param oldUid the previous UID of the accessory
     * @param newUid the new UID of the accessory
     * @param metadata additional metadata for the event
     */
    public HomekitAccessoryEvent(HomekitEventType type, HomekitAccessory accessory, HomekitAccessoryUID oldUid,
            HomekitAccessoryUID newUid, HomekitEventMetadata metadata) {
        super(type, (UID) accessory.getUID(), HomekitUID.WILDCARD_UID, metadata);
        this.accessory = Optional.ofNullable(accessory);
        this.service = Optional.empty();
        this.characteristic = Optional.empty();
        this.oldUid = Optional.of(oldUid);
        this.newUid = Optional.of(newUid);
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

    /**
     * Returns the previous UID of the accessory.
     *
     * @return an Optional containing the old UID, or empty if not a UID change event
     */
    public Optional<HomekitAccessoryUID> getOldUid() {
        return oldUid;
    }

    /**
     * Returns the new UID of the accessory.
     *
     * @return an Optional containing the new UID, or empty if not a UID change event
     */
    public Optional<HomekitAccessoryUID> getNewUid() {
        return newUid;
    }

    @Override
    public String toString() {
        return String.format(
                "HomekitAccessoryEvent{type=%s, accessory=%s, service=%s, characteristic=%s, oldUid=%s, newUid=%s, publisherUID=%s, subscriberUID=%s, timestamp=%d}",
                getType(), accessory, service, characteristic, oldUid, newUid, getPublisherUID(), getSubscriberUID(),
                getTimestamp());
    }
}
