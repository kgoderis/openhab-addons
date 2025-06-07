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

import java.util.Map;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.UID;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristic;
import org.openhab.io.homekit.api.event.HomekitEventType;
import org.openhab.io.homekit.event.core.HomekitEventMetadata;

/**
 * Event class representing an update to a HomeKit characteristic's value.
 * This event is fired when a characteristic's value is about to be changed,
 * allowing subscribers to process the change and potentially modify the configuration.
 *
 * <p>
 * The class integrates with:
 * </p>
 * <ul>
 * <li>{@link org.openhab.io.homekit.api.event.HomekitEvent} for base event functionality</li>
 * <li>{@link org.openhab.io.homekit.api.event.HomekitEventType} for event type identification</li>
 * <li>{@link org.openhab.io.homekit.api.characteristic.HomekitCharacteristic} for characteristic handling</li>
 * <li>{@link org.openhab.io.homekit.event.core.HomekitEventMetadata} for event metadata</li>
 * <li>{@link javax.json.JsonValue} for value representation</li>
 * <li>{@link java.util.Map} for configuration management</li>
 * </ul>
 *
 * <p>
 * <b>Key Features:</b>
 * </p>
 * <ul>
 * <li>Pre-update value validation</li>
 * <li>Configuration management</li>
 * <li>Value change tracking</li>
 * <li>Event metadata support</li>
 * <li>Optional value handling</li>
 * </ul>
 *
 * <p>
 * <b>Usage Patterns:</b>
 * </p>
 * <ul>
 * <li>Characteristic value updates</li>
 * <li>Configuration validation</li>
 * <li>Pre-update processing</li>
 * <li>System diagnostics</li>
 * <li>Event correlation</li>
 * </ul>
 *
 * <p>
 * <b>Configuration Management:</b>
 * </p>
 * <ul>
 * <li>Supports item configuration</li>
 * <li>Enables configuration validation</li>
 * <li>Allows configuration updates</li>
 * <li>Maintains configuration state</li>
 * </ul>
 *
 * @author Karel Goderis - Initial contribution
 * @since 3.x
 */
public class HomekitCharacteristicUpdateEvent extends HomekitCharacteristicEvent {
    private final Map<String, Object> itemConfiguration;

    /**
     * Creates a new characteristic update event with explicit publisher and subscriber UIDs.
     *
     * @param publisherUID the UID of the publisher that generated the event
     * @param subscriberUID the UID of the subscriber that will receive the event
     * @param characteristic the characteristic associated with the event
     * @param oldValue the previous value of the characteristic
     * @param newValue the new value of the characteristic
     * @param itemConfiguration the configuration map for the item
     * @param metadata additional metadata for the event
     */
    public HomekitCharacteristicUpdateEvent(UID publisherUID, UID subscriberUID,
            HomekitCharacteristic<?> characteristic, @Nullable JsonValue oldValue, @Nullable JsonValue newValue,
            Map<String, Object> itemConfiguration, HomekitEventMetadata metadata) {
        super(HomekitEventType.CHARACTERISTIC_CHANGE_VALUE, publisherUID, subscriberUID, characteristic, oldValue,
                newValue, metadata);
        this.itemConfiguration = itemConfiguration;
    }

    /**
     * Creates a new characteristic update event with default publisher and subscriber UIDs.
     *
     * @param characteristic the characteristic associated with the event
     * @param oldValue the previous value of the characteristic
     * @param newValue the new value of the characteristic
     * @param itemConfiguration the configuration map for the item
     */
    public HomekitCharacteristicUpdateEvent(HomekitCharacteristic<?> characteristic, @Nullable JsonValue oldValue,
            @Nullable JsonValue newValue, Map<String, Object> itemConfiguration) {
        super(HomekitEventType.CHARACTERISTIC_CHANGE_VALUE, characteristic, oldValue, newValue);
        this.itemConfiguration = itemConfiguration;
    }

    /**
     * Creates a new characteristic update event with metadata.
     *
     * @param characteristic the characteristic associated with the event
     * @param oldValue the previous value of the characteristic
     * @param newValue the new value of the characteristic
     * @param itemConfiguration the configuration map for the item
     * @param metadata additional metadata for the event
     */
    public HomekitCharacteristicUpdateEvent(HomekitCharacteristic<?> characteristic, @Nullable JsonValue oldValue,
            @Nullable JsonValue newValue, Map<String, Object> itemConfiguration, HomekitEventMetadata metadata) {
        super(HomekitEventType.CHARACTERISTIC_CHANGE_VALUE, characteristic, oldValue, newValue, metadata);
        this.itemConfiguration = itemConfiguration;
    }

    /**
     * Returns the configuration map for the item associated with this characteristic.
     *
     * @return the item configuration map
     */
    public Map<String, Object> getItemConfiguration() {
        return itemConfiguration;
    }
}
