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
 * HomeKit Supported Camera Recording Configuration Characteristic.
 * This characteristic represents the supported configuration for camera recording, as defined in the HomeKit Accessory
 * Protocol (HAP) specification.
 *
 * @author Karel Goderis - Initial contribution
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "00000205-0000-1000-8000-0026BB765291", name = "Supported Camera Recording Configuration", tag = "supportedCameraRecordingConfiguration", acceptedItemTypes = {
        "String" })
@NonNullByDefault
public class HomekitSupportedCameraRecordingConfigurationCharacteristic extends HomekitTLV8Characteristic {
    /**
     * Constructs a new Supported Camera Recording Configuration characteristic.
     *
     * @param service the HomeKit service this characteristic belongs to
     * @param eventManager the event manager for handling HomeKit events
     * @param instanceId the instance ID for this characteristic
     */
    public HomekitSupportedCameraRecordingConfigurationCharacteristic(HomekitService service,
            HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(false).withEvents(true)
                .withDescription("Supported Camera Recording Configuration");
    }

    /**
     * Constructs a new Supported Camera Recording Configuration characteristic from a JSON value.
     *
     * @param service the HomeKit service this characteristic belongs to
     * @param eventManager the event manager for handling HomeKit events
     * @param value the JSON value to initialize the characteristic with
     */
    public HomekitSupportedCameraRecordingConfigurationCharacteristic(HomekitService service,
            HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    /**
     * Throws UnsupportedOperationException for TLV8 encoding (must be implemented for the specific device).
     */
    @Override
    protected byte[] encodeTLV8(Map<Integer, Object> value) {
        throw new UnsupportedOperationException("TLV8 encoding must be implemented for the specific device.");
    }

    /**
     * Throws UnsupportedOperationException for TLV8 decoding (must be implemented for the specific device).
     */
    @Override
    protected Map<Integer, Object> decodeTLV8(byte[] data) {
        throw new UnsupportedOperationException("TLV8 decoding must be implemented for the specific device.");
    }

    /**
     * Throws UnsupportedOperationException for default value (must be implemented for the specific device).
     */
    @Override
    public Map<Integer, Object> getDefault() {
        return Map.of(); // Return empty map for TLV8 characteristics
    }

    /**
     * Throws UnsupportedOperationException for JSON to TLV8 conversion (must be implemented for the specific device).
     */
    @Override
    public Map<Integer, Object> toValue(JsonValue jsonValue) {
        throw new UnsupportedOperationException("JSON to TLV8 conversion must be implemented for the specific device.");
    }

    /**
     * Throws UnsupportedOperationException for State to TLV8 conversion (must be implemented for the specific device).
     */
    @Override
    public Map<Integer, Object> toValue(State state) {
        throw new UnsupportedOperationException(
                "State to TLV8 conversion must be implemented for the specific device.");
    }

    /**
     * Throws UnsupportedOperationException for TLV8 to State conversion (must be implemented for the specific device).
     */
    @Override
    public State toState(Map<Integer, Object> value) {
        throw new UnsupportedOperationException(
                "TLV8 to State conversion must be implemented for the specific device.");
    }
}
