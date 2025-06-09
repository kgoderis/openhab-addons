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
 * HomeKit Rotation Direction Characteristic.
 * This characteristic represents the direction of rotation for a device.
 * The value can be either CLOCKWISE (0) or COUNTER_CLOCKWISE (1).
 *
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "00000028-0000-1000-8000-0026BB765291", name = "Rotation Direction", tag = "rotationDirection", acceptedItemTypes = {
        "Number" })
@NonNullByDefault
public class HomekitRotationDirectionCharacteristic extends HomekitEnumCharacteristic {

    public enum RotationDirection {
        CLOCKWISE(0),
        COUNTER_CLOCKWISE(1);

        private final int value;

        RotationDirection(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static RotationDirection fromValue(int value) {
            for (RotationDirection direction : values()) {
                if (direction.value == value) {
                    return direction;
                }
            }
            throw new IllegalArgumentException("Invalid RotationDirection value: " + value);
        }
    }

    public HomekitRotationDirectionCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, 2);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(true).withEvents(true)
                .withDescription("Rotation Direction");
    }

    public HomekitRotationDirectionCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(@Nullable Integer value) {
        if (value == null)
            return false;
        try {
            RotationDirection.fromValue(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        java.util.Set<Integer> allowed = new java.util.HashSet<>();
        for (RotationDirection d : RotationDirection.values()) {
            allowed.add(d.getValue());
        }
        return allowed;
    }
}
