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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Tunnel Connection Timeout Characteristic.
 * This characteristic represents the tunnel connection timeout for a device.
 * The timeout is expressed in seconds, ranging from 0 to 3600 seconds (1 hour).
 * This is used to specify how long a tunnel connection should remain active before timing out.
 *
 * @author Karel Goderis - Initial Contribution
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "00000061-0000-1000-8000-0026BB765291", name = "Tunnel Connection Timeout", tag = "tunnelConnectionTimeout", acceptedItemTypes = {
        "Number" })
@NonNullByDefault
public class HomekitTunnelConnectionTimeoutCharacteristic extends HomekitIntegerCharacteristic {
    /**
     * Creates a new Tunnel Connection Timeout characteristic.
     * The value range is 0 to 3600 seconds.
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param instanceId The instance ID for this characteristic
     */
    public HomekitTunnelConnectionTimeoutCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, 0, 3600, "seconds");
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(true).withEvents(true)
                .withDescription("Tunnel Connection Timeout");
    }

    /**
     * Creates a new Tunnel Connection Timeout characteristic from a JSON value.
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param value The JSON value to initialize the characteristic with
     */
    public HomekitTunnelConnectionTimeoutCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(@Nullable Integer value) {
        return value != null && value >= 0 && value <= 3600;
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Collections.emptySet();
    }
}
