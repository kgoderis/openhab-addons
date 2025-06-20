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

package org.openhab.io.homekit.library.characteristic;

import java.util.Map;
import java.util.Set;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitTLV8Characteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Streaming Status Characteristic.
 * <p>
 * This characteristic represents the streaming status for a device, indicating whether streaming is available, active,
 * or busy. The value is encoded as TLV8 and must be interpreted according to the HAP specification.
 * <p>
 * See the HomeKit Accessory Protocol (HAP) specification for details: https://developer.apple.com/documentation/HomeKit
 *
 * @author Karel Goderis - Initial Contribution
 */
@HomekitCharacteristicType(type = "00000120-0000-1000-8000-0026BB765291", name = "Streaming Status", tag = "streamingStatus", acceptedItemTypes = {
        "String" })
@NonNullByDefault
public class HomekitStreamingStatusCharacteristic extends HomekitTLV8Characteristic {
    public enum StreamingStatus {
        AVAILABLE(0),
        STREAMING(1),
        BUSY(2);

        private final int value;

        StreamingStatus(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static StreamingStatus fromValue(int value) {
            for (StreamingStatus status : StreamingStatus.values()) {
                if (status.value == value) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Invalid streaming status value: " + value);
        }
    }

    /**
     * Constructs a new Streaming Status characteristic.
     *
     * @param service the HomeKit service this characteristic belongs to
     * @param eventManager the event manager for handling HomeKit events
     * @param instanceId the instance ID for this characteristic
     */
    public HomekitStreamingStatusCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(true)
                .withDescription("Streaming Status");
    }

    /**
     * Constructs a new Streaming Status characteristic from a JSON value.
     *
     * @param service the HomeKit service this characteristic belongs to
     * @param eventManager the event manager for handling HomeKit events
     * @param value the JSON value to initialize the characteristic with
     */
    public HomekitStreamingStatusCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    /**
     * Encodes the TLV8 value for the streaming status.
     *
     * @param value the value to encode
     * @return the encoded byte array
     * @throws UnsupportedOperationException always, must be implemented for the specific device
     */
    @Override
    protected byte[] encodeTLV8(Map<Integer, Object> value) {
        throw new UnsupportedOperationException("Encoding not implemented");
    }

    /**
     * Decodes the TLV8 value for the streaming status.
     *
     * @param data the byte array to decode
     * @return the decoded map
     * @throws UnsupportedOperationException always, must be implemented for the specific device
     */
    @Override
    protected Map<Integer, Object> decodeTLV8(byte[] data) {
        throw new UnsupportedOperationException("Decoding not implemented");
    }

    /**
     * Gets the default value for the streaming status.
     *
     * @return the default value map (empty for status characteristics)
     */
    @Override
    public Map<Integer, Object> getDefault() {
        return Map.of(); // Return empty map for status characteristics
    }

    /**
     * Converts a State to a TLV8 value for the streaming status.
     *
     * @param state the state to convert
     * @return the TLV8 value map
     * @throws UnsupportedOperationException always, must be implemented for the specific device
     */
    @Override
    public Map<Integer, Object> toValue(State state) {
        throw new UnsupportedOperationException("State to TLV8 not implemented");
    }

    /**
     * Converts a TLV8 value to a State for the streaming status.
     *
     * @param value the TLV8 value map
     * @return the corresponding State
     * @throws UnsupportedOperationException always, must be implemented for the specific device
     */
    @Override
    public State toState(Map<Integer, Object> value) {
        throw new UnsupportedOperationException("TLV8 to State not implemented");
    }

    /**
     * Checks if the given value is an allowed streaming status value.
     *
     * @param value the value to check
     * @return true if the value is allowed, false otherwise
     */
    @Override
    public boolean isAllowedValue(@Nullable Map<Integer, Object> value) {
        // Implement a proper check if you know the allowed values, otherwise:
        return true;
    }

    /**
     * Returns the set of allowed streaming status values.
     *
     * @return the set of allowed values
     */
    @Override
    public Set<Map<Integer, Object>> getAllowedValues() {
        return Set.of();
    }
}
