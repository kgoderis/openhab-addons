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

import org.openhab.core.types.State;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitTLV8Characteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit App Matching Identifier Characteristic.
 * This characteristic represents the app matching identifier as a TLV8 value.
 *
 * @see <a href=\"https://developer.apple.com/documentation/HomeKit\">HAP Specification</a>
 * @author Karel Goderis - Initial contribution
     */
@HomekitCharacteristicType(type = "000000A4-0000-1000-8000-0026BB765291", name = "App Matching Identifier", tag = "appMatchingIdentifier", acceptedItemTypes = {
        "String" })
public class HomekitAppMatchingIdentifierCharacteristic extends HomekitTLV8Characteristic {

    public HomekitAppMatchingIdentifierCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(true)
                .withDescription("App Matching Identifier");
    }

    public HomekitAppMatchingIdentifierCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    /**
     * Encodes the TLV8 value for the app matching identifier.
     *
     * @param value the value to encode
     * @return the encoded byte array
     */
    @Override
    protected byte[] encodeTLV8(java.util.Map<Integer, Object> value) {
        return new byte[0];
    }

    /**
     * Decodes the TLV8 value for the app matching identifier.
     *
     * @param data the byte array to decode
     * @return the decoded map
     */
    @Override
    protected java.util.Map<Integer, Object> decodeTLV8(byte[] data) {
        return java.util.Collections.emptyMap();
    }

    /**
     * Gets the default value for the app matching identifier.
     *
     * @return the default value map
     */
    @Override
    public java.util.Map<Integer, Object> getDefault() {
        return java.util.Collections.emptyMap();
    }

    /**
     * Converts a State to a TLV8 value for the app matching identifier.
     *
     * @param state the state to convert
     * @return the TLV8 value map
     */
    @Override
    public java.util.Map<Integer, Object> toValue(org.openhab.core.types.State state) {
        return java.util.Collections.emptyMap();
    }

    /**
     * Converts a TLV8 value to a State for the app matching identifier.
     *
     * @param value the TLV8 value map
     * @return the corresponding State
     */
    @Override
    public State toState(java.util.Map<Integer, Object> value) {
        throw new UnsupportedOperationException(
                "TLV8 to State conversion must be implemented for the specific device.");
    }
}
