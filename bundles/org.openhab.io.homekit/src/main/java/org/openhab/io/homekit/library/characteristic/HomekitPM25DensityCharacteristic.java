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
import org.openhab.io.homekit.core.characteristic.HomekitFloatCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit PM2.5 Density Characteristic.
 * This characteristic represents the PM2.5 density in micrograms per cubic meter.
 *
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 * @author Karel Goderis - Initial contribution
 */
@HomekitCharacteristicType(type = "000000C6-0000-1000-8000-0026BB765291", name = "PM2.5 Density", tag = "pm25Density", acceptedItemTypes = {
        "Number" })
@NonNullByDefault
public class HomekitPM25DensityCharacteristic extends HomekitFloatCharacteristic {
    /**
     * Creates a new PM2.5 Density characteristic.
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param instanceId The instance ID for this characteristic
     */
    public HomekitPM25DensityCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 0.0, 1000.0, 1.0, "microgramsPerCubicMeter");
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(true)
                .withDescription("PM2.5 Density");
    }

    /**
     * Creates a new PM2.5 Density characteristic from a JSON value.
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param value The JSON value to initialize the characteristic with
     */
    public HomekitPM25DensityCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    /**
     * Checks if the given value is a valid PM2.5 density.
     *
     * @param value the double value to check
     * @return true if the value is between 0.0 and 1000.0, false otherwise
     */
    @Override
    public boolean isAllowedValue(@Nullable Double value) {
        return value != null && value >= 0.0 && value <= 000.0;
    }

    /**
     * Gets the set of all valid PM2.5 density values (empty for continuous range).
     *
     * @return an empty set
     */
    @Override
    public java.util.Set<Double> getAllowedValues() {
        return java.util.Collections.emptySet();
    }
}
