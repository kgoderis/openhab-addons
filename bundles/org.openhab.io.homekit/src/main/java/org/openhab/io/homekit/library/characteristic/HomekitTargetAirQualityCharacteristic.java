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
 * HomeKit Target Air Quality Characteristic.
 * This characteristic represents the target air quality for a device.
 * The quality can be one of: EXCELLENT, GOOD, FAIR, INFERIOR, or POOR.
 *
 * @author Karel Goderis - Initial Contribution
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "000000AE-0000-1000-8000-0026BB765291", name = "Target Air Quality", tag = "targetAirQuality", acceptedItemTypes = {
        "Number", "String" })
@NonNullByDefault
public class HomekitTargetAirQualityCharacteristic extends HomekitEnumCharacteristic {
    public enum TargetAirQuality {
        EXCELLENT(0),
        GOOD(1),
        FAIR(2),
        INFERIOR(3),
        POOR(4);

        private final int value;

        TargetAirQuality(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static TargetAirQuality fromValue(int value) {
            for (TargetAirQuality quality : values()) {
                if (quality.value == value) {
                    return quality;
                }
            }
            throw new IllegalArgumentException("Invalid Target Air Quality value: " + value);
        }
    }

    public HomekitTargetAirQualityCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, TargetAirQuality.values().length);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(true).withEvents(true)
                .withDescription("Target Air Quality");
    }

    public HomekitTargetAirQualityCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(@Nullable Integer value) {
        return value != null && value >= TargetAirQuality.EXCELLENT.getValue()
                && value <= TargetAirQuality.POOR.getValue();
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(TargetAirQuality.EXCELLENT.getValue(), TargetAirQuality.GOOD.getValue(),
                TargetAirQuality.FAIR.getValue(), TargetAirQuality.INFERIOR.getValue(),
                TargetAirQuality.POOR.getValue());
    }

    public void setValue(TargetAirQuality value) {
        try {
            setValue(value.getValue());
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to set Target Air Quality value", e);
        }
    }
}
