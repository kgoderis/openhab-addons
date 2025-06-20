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
 * HomeKit Supported Audio Stream Configuration Characteristic.
 * <p>
 * This characteristic represents the supported audio stream configuration for a device, such as a camera or speaker.
 * The value is encoded as TLV8 and must be interpreted according to the HAP specification.
 * <p>
 * See the HomeKit Accessory Protocol (HAP) specification for details: https://developer.apple.com/documentation/HomeKit
 *
 * @author Karel Goderis - Initial Contribution
 */
@HomekitCharacteristicType(type = "00000115-0000-1000-8000-0026BB765291", name = "Supported Audio Stream Configuration", tag = "supportedAudioStreamConfiguration", acceptedItemTypes = {
        "String" })
@NonNullByDefault
public class HomekitSupportedAudioStreamConfigurationCharacteristic extends HomekitTLV8Characteristic {

    /**
     * Constructs a new Supported Audio Stream Configuration characteristic.
     *
     * @param service the HomeKit service this characteristic belongs to
     * @param eventManager the event manager for handling HomeKit events
     * @param instanceId the instance ID for this characteristic
     */
    public HomekitSupportedAudioStreamConfigurationCharacteristic(HomekitService service,
            HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId).withPairedRead(true).withDescription("Supported Audio Stream Configuration");
    }

    /**
     * Constructs a new Supported Audio Stream Configuration characteristic from a JSON value.
     *
     * @param service the HomeKit service this characteristic belongs to
     * @param eventManager the event manager for handling HomeKit events
     * @param value the JSON value to initialize the characteristic with
     */
    public HomekitSupportedAudioStreamConfigurationCharacteristic(HomekitService service,
            HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    /**
     * Encodes the TLV8 value for the supported audio stream configuration.
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
     * Decodes the TLV8 value for the supported audio stream configuration.
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
     * Gets the default value for the supported audio stream configuration.
     *
     * @return the default value map
     * @throws UnsupportedOperationException always, must be implemented for the specific device
     */
    @Override
    public Map<Integer, Object> getDefault() {
        return Map.of(); // Return empty map for TLV8 characteristics
    }

    /**
     * Converts a JSON value to a TLV8 value for the supported audio stream configuration.
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
     * Converts a State to a TLV8 value for the supported audio stream configuration.
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
     * Converts a TLV8 value to a State for the supported audio stream configuration.
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
