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
 * HomeKit Configuration State Characteristic.
 * <p>
 * This characteristic represents the configuration state in TLV8 format. It is used to indicate the current
 * configuration status of the accessory, such as whether it is properly configured or requires user action. The value
 * is encoded as TLV8 and must be interpreted according to the device's capabilities and the HAP specification.
 * <p>
 * See the HomeKit Accessory Protocol (HAP) specification for details: https://developer.apple.com/documentation/HomeKit
 *
 * @author Karel Goderis - Initial Contribution
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "00000263-0000-1000-8000-0026BB765291", name = "Configuration State", tag = "configurationState", acceptedItemTypes = {
        "String" })
@NonNullByDefault
public class HomekitConfigurationStateCharacteristic extends HomekitTLV8Characteristic {
    /**
     * Constructs a new Configuration State characteristic.
     *
     * @param service the HomeKit service this characteristic belongs to
     * @param eventManager the event manager for handling HomeKit events
     * @param instanceId the instance ID for this characteristic
     */
    public HomekitConfigurationStateCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(false).withEvents(true)
                .withDescription("Configuration State");
    }

    /**
     * Constructs a new Configuration State characteristic from a JSON value.
     *
     * @param service the HomeKit service this characteristic belongs to
     * @param eventManager the event manager for handling HomeKit events
     * @param value the JSON value to initialize the characteristic with
     */
    public HomekitConfigurationStateCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    /**
     * Encodes the TLV8 value for the configuration state.
     *
     * @param value the value to encode
     * @return the encoded byte array
     * @throws UnsupportedOperationException always, must be implemented for the specific device
     */
    @Override
    protected byte[] encodeTLV8(Map<Integer, Object> value) {
        throw new UnsupportedOperationException("TLV8 encoding must be implemented for the specific device.");
    }

    /**
     * Decodes the TLV8 value for the configuration state.
     *
     * @param data the byte array to decode
     * @return the decoded map
     * @throws UnsupportedOperationException always, must be implemented for the specific device
     */
    @Override
    protected Map<Integer, Object> decodeTLV8(byte[] data) {
        throw new UnsupportedOperationException("TLV8 decoding must be implemented for the specific device.");
    }

    /**
     * Gets the default value for the configuration state.
     *
     * @return the default value map
     * @throws UnsupportedOperationException always, must be implemented for the specific device
     */
    @Override
    public Map<Integer, Object> getDefault() {
        throw new UnsupportedOperationException("Default value must be implemented for the specific device.");
    }

    /**
     * Converts a JSON value to a TLV8 value for the configuration state.
     *
     * @param jsonValue the JSON value to convert
     * @return the TLV8 value map
     * @throws UnsupportedOperationException always, must be implemented for the specific device
     */
    @Override
    public Map<Integer, Object> toValue(JsonValue jsonValue) {
        throw new UnsupportedOperationException("JSON to TLV8 conversion must be implemented for the specific device.");
    }

    /**
     * Converts a State to a TLV8 value for the configuration state.
     *
     * @param state the state to convert
     * @return the TLV8 value map
     * @throws UnsupportedOperationException always, must be implemented for the specific device
     */
    @Override
    public Map<Integer, Object> toValue(State state) {
        throw new UnsupportedOperationException(
                "State to TLV8 conversion must be implemented for the specific device.");
    }

    /**
     * Converts a TLV8 value to a State for the configuration state.
     *
     * @param value the TLV8 value map
     * @return the corresponding State
     * @throws UnsupportedOperationException always, must be implemented for the specific device
     */
    @Override
    public State toState(Map<Integer, Object> value) {
        throw new UnsupportedOperationException(
                "TLV8 to State conversion must be implemented for the specific device.");
    }
}
