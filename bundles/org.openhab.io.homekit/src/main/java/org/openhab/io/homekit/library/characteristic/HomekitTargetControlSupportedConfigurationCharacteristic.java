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
 * HomeKit Target Control Supported Configuration Characteristic.
 * <p>
 * This characteristic represents the supported configuration for target control, as defined in the HomeKit Accessory
 * Protocol (HAP) specification.
 * <p>
 * See the HomeKit Accessory Protocol (HAP) specification for details: https://developer.apple.com/documentation/HomeKit
 *
 * @author Karel Goderis - Initial Contribution
 */
@HomekitCharacteristicType(type = "00000123-0000-1000-8000-0026BB765291", name = "Target Control Supported Configuration", tag = "targetControlSupportedConfiguration", acceptedItemTypes = {
        "String" })
@NonNullByDefault
public class HomekitTargetControlSupportedConfigurationCharacteristic extends HomekitTLV8Characteristic {

    /**
     * Constructs the characteristic for target control supported configuration.
     *
     * @param service the Homekit service
     * @param eventManager the event manager
     * @param instanceId the instance id
     */
    public HomekitTargetControlSupportedConfigurationCharacteristic(HomekitService service,
            HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId).withPairedRead(true).withDescription("Target Control Supported Configuration");
    }

    /**
     * Constructs the characteristic from a JSON value.
     *
     * @param service the Homekit service
     * @param eventManager the event manager
     * @param value the JSON value
     */
    public HomekitTargetControlSupportedConfigurationCharacteristic(HomekitService service,
            HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    /**
     * Encodes the TLV8 value for this characteristic.
     * 
     * @param value the value to encode
     * @return the encoded byte array
     */
    @Override
    protected byte[] encodeTLV8(Map<Integer, Object> value) {
        throw new UnsupportedOperationException("TLV8 encoding must be implemented for the specific device.");
    }

    /**
     * Decodes the TLV8 value for this characteristic.
     * 
     * @param data the byte array to decode
     * @return the decoded value as a map
     */
    @Override
    protected Map<Integer, Object> decodeTLV8(byte[] data) {
        throw new UnsupportedOperationException("TLV8 decoding must be implemented for the specific device.");
    }

    /**
     * Returns the default value for this characteristic.
     * 
     * @return the default value as a map
     */
    @Override
    public Map<Integer, Object> getDefault() {
        return Map.of(); // Return empty map for TLV8 characteristics
    }

    /**
     * Converts a JSON value to a TLV8 value.
     * 
     * @param jsonValue the JSON value
     * @return the TLV8 value as a map
     */
    @Override
    public Map<Integer, Object> toValue(JsonValue jsonValue) {
        throw new UnsupportedOperationException("JSON to TLV8 conversion must be implemented for the specific device.");
    }

    /**
     * Converts a State to a TLV8 value.
     * 
     * @param state the state
     * @return the TLV8 value as a map
     */
    @Override
    public Map<Integer, Object> toValue(State state) {
        throw new UnsupportedOperationException(
                "State to TLV8 conversion must be implemented for the specific device.");
    }

    /**
     * Converts a TLV8 value to a State.
     * 
     * @param value the TLV8 value as a map
     * @return the state
     */
    @Override
    public State toState(Map<Integer, Object> value) {
        throw new UnsupportedOperationException(
                "TLV8 to State conversion must be implemented for the specific device.");
    }
}
