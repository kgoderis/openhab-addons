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

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitTLV8Characteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Lock Management Control Point Characteristic.
 * <p>
 * This characteristic represents the lock management control point for a device, used to send control commands to the
 * lock management system. The value is encoded as TLV8 and must be interpreted according to the HAP specification.
 * <p>
 * See the HomeKit Accessory Protocol (HAP) specification for details: https://developer.apple.com/documentation/HomeKit
 *
 * @author Karel Goderis - Initial Contribution
 */
@HomekitCharacteristicType(type = "0000011C-0000-1000-8000-0026BB765291", name = "Lock Management Control Point", tag = "lockManagementControlPoint", acceptedItemTypes = {
        "String" })
@NonNullByDefault
public class HomekitLockManagementControlPointCharacteristic extends HomekitTLV8Characteristic {
    /**
     * Constructs a new Lock Management Control Point characteristic.
     *
     * @param service the HomeKit service this characteristic belongs to
     * @param eventManager the event manager for handling HomeKit events
     * @param instanceId the instance ID for this characteristic
     */
    public HomekitLockManagementControlPointCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId).withPairedWrite(true).withPairedRead(true).withEvents(false)
                .withDescription("Lock Management Control Point");
    }

    /**
     * Constructs a new Lock Management Control Point characteristic from a JSON value.
     *
     * @param service the HomeKit service this characteristic belongs to
     * @param eventManager the event manager for handling HomeKit events
     * @param value the JSON value to initialize the characteristic with
     */
    public HomekitLockManagementControlPointCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    /**
     * Encodes the TLV8 value for the lock management control point.
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
     * Decodes the TLV8 value for the lock management control point.
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
     * Gets the default value for the lock management control point.
     *
     * @return the default value map (empty for control point characteristics)
     */
    @Override
    public Map<Integer, Object> getDefault() {
        return Map.of(); // Return empty map for control point characteristics
    }

    /**
     * Converts a State to a TLV8 value for the lock management control point.
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
     * Converts a TLV8 value to a State for the lock management control point.
     *
     * @param value the TLV8 value map
     * @return the corresponding State
     * @throws UnsupportedOperationException always, must be implemented for the specific device
     */
    @Override
    public State toState(Map<Integer, Object> value) {
        throw new UnsupportedOperationException("toState not implemented");
    }
}
