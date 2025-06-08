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
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Input Device Type Characteristic.
 * This characteristic represents the type of input device.
 * The type can be one of: OTHER, KEYBOARD, MOUSE, TOUCHPAD, or GAMEPAD.
 *
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "000000DC-0000-1000-8000-0026BB765291", name = "Input Device Type", tag = "inputDeviceType", acceptedItemTypes = {
        "Number", "String" })
@NonNullByDefault
public class HomekitInputDeviceTypeCharacteristic extends HomekitEnumCharacteristic {
    public enum InputDeviceType {
        OTHER(0),
        KEYBOARD(1),
        MOUSE(2),
        TOUCHPAD(3),
        GAMEPAD(4);

        private final int value;

        InputDeviceType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static InputDeviceType fromValue(int value) {
            for (InputDeviceType type : values()) {
                if (type.value == value) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Invalid Input Device Type value: " + value);
        }
    }

    public HomekitInputDeviceTypeCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, InputDeviceType.values().length);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(false).withEvents(true)
                .withDescription("Input Device Type");
    }

    public HomekitInputDeviceTypeCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        if (value == null) {
            return false;
        }
        try {
            InputDeviceType.fromValue(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(InputDeviceType.OTHER.getValue(), InputDeviceType.KEYBOARD.getValue(),
                InputDeviceType.MOUSE.getValue(), InputDeviceType.TOUCHPAD.getValue(),
                InputDeviceType.GAMEPAD.getValue());
    }
}
