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

import javax.json.JsonValue;

import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitDataCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Binary Data Characteristic.
 * This characteristic represents binary data that can be read from or written to a device.
 * It is used for raw data transfer between the device and HomeKit.
 *
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
     */
@HomekitCharacteristicType(type = "000000D2-0000-1000-8000-0026BB765291", name = "Data", tag = "data", acceptedItemTypes = {
        "String" })
public class HomekitBinaryDataCharacteristic extends HomekitDataCharacteristic {
    public HomekitBinaryDataCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(true).withEvents(true).withDescription("Data");
    }

    public HomekitBinaryDataCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    // Optionally override encodeData/decodeData if needed
    // Optionally override toValue/toState if needed

    @Override
    public boolean isAllowedValue(byte[] value) {
        return value != null; // All non-null binary data is allowed
    }

    @Override
    public java.util.Set<byte[]> getAllowedValues() {
        return java.util.Collections.emptySet(); // No specific allowed values
    }

    @Override
    public byte[] getDefault() {
        return new byte[0];
    }

    @Override
    public byte[] toValue(org.openhab.core.types.State state) {
        throw new UnsupportedOperationException("State to binary data conversion not implemented");
    }

    @Override
    public org.openhab.core.types.State toState(byte[] value) {
        throw new UnsupportedOperationException("Binary data to State conversion not implemented");
    }
}
