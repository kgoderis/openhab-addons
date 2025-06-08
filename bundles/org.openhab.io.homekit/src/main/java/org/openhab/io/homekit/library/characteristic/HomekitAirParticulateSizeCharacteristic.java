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

import java.util.HashSet;
import java.util.Set;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Air Particulate Size Characteristic.
 * <p>
 * This characteristic represents the particulate size measured by an air quality sensor, such as PM2.5 or PM10. The
 * value is an integer corresponding to a specific particulate size as defined by the HAP specification.
 * <p>
 * See the HomeKit Accessory Protocol (HAP) specification for details: https://developer.apple.com/documentation/HomeKit
 *
 * @author Karel Goderis
 */
@HomekitCharacteristicType(type = "00000065-0000-1000-8000-0026BB765291", name = "Air Particulate Size", tag = "airParticulateSize", acceptedItemTypes = {
        "Number", "String" })
@NonNullByDefault
public class HomekitAirParticulateSizeCharacteristic extends HomekitIntegerCharacteristic {

    /**
     * Enum representing the possible particulate sizes.
     */
    public enum AirParticulateSize {
        SIZE_2_5_M(0),
        SIZE_10_M(1);

        private final int code;

        AirParticulateSize(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static AirParticulateSize fromCode(int code) {
            for (AirParticulateSize s : values()) {
                if (s.code == code)
                    return s;
            }
            return SIZE_2_5_M;
        }
    }

    /**
     * Constructs a new Air Particulate Size characteristic.
     *
     * @param service the HomeKit service this characteristic belongs to
     * @param eventManager the event manager for handling HomeKit events
     * @param instanceId the instance ID for this characteristic
     */
    public HomekitAirParticulateSizeCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, 0, 1, "");
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(true)
                .withDescription("Air Particulate Size");
    }

    /**
     * Constructs a new Air Particulate Size characteristic from a JSON value.
     *
     * @param service the HomeKit service this characteristic belongs to
     * @param eventManager the event manager for handling HomeKit events
     * @param value the JSON value to initialize the characteristic with
     */
    public HomekitAirParticulateSizeCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    /**
     * Checks if the given value is an allowed particulate size.
     *
     * @param value the value to check
     * @return true if the value is allowed, false otherwise
     */
    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && (value == 0 || value == 1);
    }

    /**
     * Returns the set of allowed particulate size values.
     *
     * @return the set of allowed values
     */
    @Override
    public Set<Integer> getAllowedValues() {
        Set<Integer> set = new HashSet<>();
        for (AirParticulateSize s : AirParticulateSize.values()) {
            set.add(s.getCode());
        }
        return set;
    }
}
