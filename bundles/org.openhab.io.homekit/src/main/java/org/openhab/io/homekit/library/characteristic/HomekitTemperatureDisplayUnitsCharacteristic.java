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
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Temperature Display Units Characteristic.
 * This characteristic represents the temperature display units for a device.
 * The units can be one of: CELSIUS (0) or FAHRENHEIT (1).
 * This is used to specify whether temperature values should be displayed in Celsius or Fahrenheit.
 *
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "00000036-0000-1000-8000-0026BB765291", name = "Temperature Display Units", tag = "temperatureDisplayUnits", acceptedItemTypes = {
        "Number", "String" })
@NonNullByDefault
public class HomekitTemperatureDisplayUnitsCharacteristic extends HomekitEnumCharacteristic {
    public enum TemperatureDisplayUnits {
        CELSIUS(0),
        FAHRENHEIT(1);

        private final int value;

        TemperatureDisplayUnits(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static TemperatureDisplayUnits fromValue(int value) {
            for (TemperatureDisplayUnits units : values()) {
                if (units.value == value) {
                    return units;
                }
            }
            throw new IllegalArgumentException("Invalid Temperature Display Units value: " + value);
        }
    }

    public HomekitTemperatureDisplayUnitsCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, 2);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(true).withEvents(true)
                .withDescription("Temperature Display Units");
    }

    public HomekitTemperatureDisplayUnitsCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(@Nullable Integer value) {
        try {
            return value != null && TemperatureDisplayUnits.fromValue(value) != null;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        java.util.Set<Integer> values = new java.util.HashSet<>();
        for (TemperatureDisplayUnits units : TemperatureDisplayUnits.values()) {
            values.add(units.getValue());
        }
        return values;
    }

    public void setValue(TemperatureDisplayUnits value) {
        try {
            setValue(value.getValue());
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to set Temperature Display Units value", e);
        }
    }
}
