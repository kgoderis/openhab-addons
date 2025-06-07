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

package org.openhab.io.homekit.event.model.characteristic;

import java.util.Collections;
import java.util.Optional;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.UID;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristic;
import org.openhab.io.homekit.api.event.HomekitEventType;
import org.openhab.io.homekit.event.core.AbstractHomekitEvent;
import org.openhab.io.homekit.event.core.HomekitEventMetadata;
import org.openhab.io.homekit.util.HomekitUID;

/**
 * Base event class for all HomeKit characteristic-related events.
 * This class provides common functionality for handling characteristic value changes,
 * including tracking old and new values, and managing event metadata.
 *
 * <p>
 * The class integrates with:
 * </p>
 * <ul>
 * <li>{@link org.openhab.io.homekit.api.event.HomekitEvent} for base event functionality</li>
 * <li>{@link org.openhab.io.homekit.api.event.HomekitEventType} for event type identification</li>
 * <li>{@link org.openhab.io.homekit.api.characteristic.HomekitCharacteristic} for characteristic handling</li>
 * <li>{@link org.openhab.io.homekit.event.core.HomekitEventMetadata} for event metadata</li>
 * <li>{@link org.openhab.io.homekit.util.HomekitUID} for UID generation and management</li>
 * <li>{@link javax.json.JsonValue} for value representation</li>
 * </ul>
 *
 * <p>
 * <b>Key Features:</b>
 * </p>
 * <ul>
 * <li>Value change tracking with old and new values</li>
 * <li>Characteristic identification and management</li>
 * <li>Event metadata support</li>
 * <li>Optional value handling</li>
 * <li>UID-based routing</li>
 * </ul>
 *
 * <p>
 * <b>Usage Patterns:</b>
 * </p>
 * <ul>
 * <li>Characteristic value updates</li>
 * <li>State change notifications</li>
 * <li>Value history tracking</li>
 * <li>System diagnostics</li>
 * <li>Event correlation</li>
 * </ul>
 *
 * <p>
 * <b>Value Management:</b>
 * </p>
 * <ul>
 * <li>Supports JSON value representation</li>
 * <li>Maintains value history</li>
 * <li>Enables value validation</li>
 * <li>Supports metadata enrichment</li>
 * </ul>
 *
 * @author Karel Goderis - Initial contribution
 * @since 3.x
 */
public class HomekitCharacteristicEvent extends AbstractHomekitEvent {
    private final Optional<HomekitCharacteristic<?>> characteristic;
    private final Optional<JsonValue> oldValue;
    private final Optional<JsonValue> newValue;

    /**
     * Creates a new characteristic event with explicit publisher and subscriber UIDs.
     *
     * @param type the type of event
     * @param publisherUID the UID of the publisher that generated the event
     * @param subscriberUID the UID of the subscriber that will receive the event
     * @param characteristic the characteristic associated with the event
     * @param oldValue the previous value of the characteristic
     * @param newValue the new value of the characteristic
     * @param metadata additional metadata for the event
     */
    @SuppressWarnings("null")
    public HomekitCharacteristicEvent(HomekitEventType type, UID publisherUID, UID subscriberUID,
            HomekitCharacteristic<?> characteristic, @Nullable JsonValue oldValue, @Nullable JsonValue newValue,
            HomekitEventMetadata metadata) {
        super(type, publisherUID, subscriberUID, metadata);
        this.characteristic = Optional.ofNullable(characteristic);
        this.oldValue = Optional.ofNullable(oldValue);
        this.newValue = Optional.ofNullable(newValue);
    }

    /**
     * Creates a new characteristic event with default publisher and subscriber UIDs.
     *
     * @param type the type of event
     * @param characteristic the characteristic associated with the event
     * @param oldValue the previous value of the characteristic
     * @param newValue the new value of the characteristic
     */
    @SuppressWarnings("null")
    public HomekitCharacteristicEvent(HomekitEventType type, HomekitCharacteristic<?> characteristic,
            @Nullable JsonValue oldValue, @Nullable JsonValue newValue) {
        super(type, characteristic != null ? (UID) characteristic.getUID() : (UID) new HomekitUID("characteristic"),
                HomekitUID.WILDCARD_UID,
                new HomekitEventMetadata(
                        characteristic != null ? (UID) characteristic.getUID() : (UID) new HomekitUID("characteristic"),
                        (UID) null, (UID) null, Collections.emptySet()));
        this.characteristic = Optional.ofNullable(characteristic);
        this.oldValue = Optional.ofNullable(oldValue);
        this.newValue = Optional.ofNullable(newValue);
    }

    /**
     * Creates a new characteristic event with metadata.
     *
     * @param type the type of event
     * @param characteristic the characteristic associated with the event
     * @param oldValue the previous value of the characteristic
     * @param newValue the new value of the characteristic
     * @param metadata additional metadata for the event
     */
    @SuppressWarnings("null")
    public HomekitCharacteristicEvent(HomekitEventType type, HomekitCharacteristic<?> characteristic,
            @Nullable JsonValue oldValue, @Nullable JsonValue newValue, HomekitEventMetadata metadata) {
        super(type, characteristic != null ? (UID) characteristic.getUID() : (UID) new HomekitUID("characteristic"),
                HomekitUID.WILDCARD_UID, metadata);
        this.characteristic = Optional.ofNullable(characteristic);
        this.oldValue = Optional.ofNullable(oldValue);
        this.newValue = Optional.ofNullable(newValue);
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
     * Returns the previous value of the characteristic.
     *
     * @return an Optional containing the old value, or empty if not available
     */
    public Optional<JsonValue> getOldValue() {
        return oldValue;
    }

    /**
     * Returns the new value of the characteristic.
     *
     * @return an Optional containing the new value, or empty if not available
     */
    public Optional<JsonValue> getNewValue() {
        return newValue;
    }

    @Override
    public String toString() {
        return String.format(
                "HomekitCharacteristicEvent{type=%s, characteristic=%s, oldValue=%s, newValue=%s, publisherUID=%s, subscriberUID=%s, timestamp=%d}",
                getType(), characteristic, oldValue, newValue, getPublisherUID(), getSubscriberUID(), getTimestamp());
    }
}
