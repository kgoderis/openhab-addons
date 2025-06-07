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

import org.openhab.core.types.State;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitTLV8Characteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Pair Setup Characteristic.
 * <p>
 * This characteristic represents the TLV8 value for HomeKit pair setup. It is used during the pairing process to
 * securely exchange credentials between the accessory and the HomeKit controller. The value is encoded as TLV8 and must
 * be interpreted according to the HAP specification.
 * <p>
 * See the HomeKit Accessory Protocol (HAP) specification for details: https://developer.apple.com/documentation/HomeKit
 *
 * @author Karel Goderis
 */
@HomekitCharacteristicType(type = "0000004C-0000-1000-8000-0026BB765291", name = "Pair Setup", tag = "pairSetup", acceptedItemTypes = {
        "String" })
public class HomekitPairSetupCharacteristic extends HomekitTLV8Characteristic {

    /**
     * Constructs a new Pair Setup characteristic.
     *
     * @param service the HomeKit service this characteristic belongs to
     * @param eventManager the event manager for handling HomeKit events
     * @param instanceId the instance ID for this characteristic
     */
    public HomekitPairSetupCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId).withPairedWrite(true).withPairedRead(true).withEvents(true)
                .withDescription("Pair Setup");
    }

    /**
     * Constructs a new Pair Setup characteristic from a JSON value.
     *
     * @param service the HomeKit service this characteristic belongs to
     * @param eventManager the event manager for handling HomeKit events
     * @param value the JSON value to initialize the characteristic with
     */
    public HomekitPairSetupCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    /**
     * Encodes the TLV8 value for pair setup.
     *
     * @param value the value to encode
     * @return the encoded byte array
     */
    @Override
    protected byte[] encodeTLV8(Map<Integer, Object> value) {
        return new byte[0];
    }

    /**
     * Decodes the TLV8 value for pair setup.
     *
     * @param data the byte array to decode
     * @return the decoded map
     */
    @Override
    protected Map<Integer, Object> decodeTLV8(byte[] data) {
        return java.util.Collections.emptyMap();
    }

    /**
     * Gets the default value for pair setup.
     *
     * @return the default value map
     */
    @Override
    public Map<Integer, Object> getDefault() {
        return java.util.Collections.emptyMap();
    }

    /**
     * Converts a State to a TLV8 value for pair setup.
     *
     * @param state the state to convert
     * @return the TLV8 value map
     */
    @Override
    public Map<Integer, Object> toValue(State state) {
        return java.util.Collections.emptyMap();
    }

    /**
     * Converts a TLV8 value to a State for pair setup.
     *
     * @param value the TLV8 value map
     * @return the corresponding State
     */
    @Override
    public State toState(Map<Integer, Object> value) {
        throw new UnsupportedOperationException(
                "TLV8 to State conversion must be implemented for the specific device.");
    }
}
