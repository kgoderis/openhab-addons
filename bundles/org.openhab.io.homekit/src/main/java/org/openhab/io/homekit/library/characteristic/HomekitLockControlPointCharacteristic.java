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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitTLV8Characteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Lock Control Point Characteristic.
 * This characteristic is used to send lock control commands to a lock device.
 * The TLV8 format is used to encode the control commands.
 *
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/homekit/hap-characteristic-types/lock-control-point">HAP
 *      Specification</a>
 */
@HomekitCharacteristicType(type = "00000019-0000-1000-8000-0026BB765291", name = "Lock Control Point", tag = "lockControlPoint", acceptedItemTypes = {
        "String" })
@NonNullByDefault
public class HomekitLockControlPointCharacteristic extends HomekitTLV8Characteristic {
    public static final int LOCK_OPERATION = 1;
    public static final int ADMIN_CLEAR_CREDENTIALS = 2;
    public static final int GET_CREDENTIALS = 3;
    public static final int GET_CREDENTIALS_RESPONSE = 4;
    public static final int GET_CREDENTIALS_STATUS = 5;
    public static final int GET_CREDENTIALS_STATUS_RESPONSE = 6;

    public HomekitLockControlPointCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId).withPairedWrite(true).withPairedRead(true).withEvents(true)
                .withDescription("Lock Control Point");
    }

    public HomekitLockControlPointCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    protected byte[] encodeTLV8(Map<Integer, Object> value) {
        if (value == null || value.isEmpty()) {
            return new byte[0];
        }

        // Basic TLV8 encoding implementation
        // In a real implementation, this would properly encode the TLV8 format
        // based on the HAP specification
        return new byte[0];
    }

    @Override
    protected Map<Integer, Object> decodeTLV8(byte[] data) {
        if (data == null || data.length == 0) {
            return Collections.emptyMap();
        }

        // Basic TLV8 decoding implementation
        // In a real implementation, this would properly decode the TLV8 format
        // based on the HAP specification
        return new HashMap<>();
    }

    @Override
    public Map<Integer, Object> getDefault() {
        return Collections.emptyMap();
    }

    @Override
    public Map<Integer, Object> toValue(State state) {
        throw new UnsupportedOperationException(
                "State to TLV8 conversion must be implemented for the specific device.");
    }

    @Override
    public State toState(Map<Integer, Object> value) {
        throw new UnsupportedOperationException(
                "TLV8 to State conversion must be implemented for the specific device.");
    }
}
