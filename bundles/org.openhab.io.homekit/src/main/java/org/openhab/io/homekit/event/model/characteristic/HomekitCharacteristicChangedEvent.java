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

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristic;
import org.openhab.io.homekit.api.event.HomekitEventType;
import org.openhab.io.homekit.event.core.HomekitEventMetadata;

/**
 * Represents a characteristic value changed event in the HomeKit integration.
 * This event is published when a characteristic's value has been changed.
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
 * </ul>
 *
 * <p>
 * <b>Key Features:</b>
 * </p>
 * <ul>
 * <li>Value change tracking with old and new values</li>
 * <li>Characteristic identification</li>
 * <li>Timestamp tracking</li>
 * <li>Metadata support</li>
 * </ul>
 *
 * <p>
 * <b>Usage Patterns:</b>
 * </p>
 * <ul>
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
@NonNullByDefault
public class HomekitCharacteristicChangedEvent extends HomekitCharacteristicEvent {

    /**
     * Creates a new characteristic changed event.
     *
     * @param characteristic the characteristic that changed
     * @param oldValue the previous value of the characteristic
     * @param newValue the new value of the characteristic
     */
    public HomekitCharacteristicChangedEvent(HomekitCharacteristic<?> characteristic, @Nullable JsonValue oldValue,
            @Nullable JsonValue newValue) {
        super(HomekitEventType.CHARACTERISTIC_VALUE_CHANGED, characteristic, oldValue, newValue);
    }

    /**
     * Creates a new characteristic changed event with metadata.
     *
     * @param characteristic the characteristic that changed
     * @param oldValue the previous value of the characteristic
     * @param newValue the new value of the characteristic
     * @param metadata additional metadata for the event
     */
    public HomekitCharacteristicChangedEvent(HomekitCharacteristic<?> characteristic, @Nullable JsonValue oldValue,
            @Nullable JsonValue newValue, HomekitEventMetadata metadata) {
        super(HomekitEventType.CHARACTERISTIC_VALUE_CHANGED, characteristic, oldValue, newValue, metadata);
    }
}
