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

import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Swing Mode Characteristic.
 * This characteristic represents the swing mode for a device (e.g., fan or air conditioner).
 * The mode can be one of: SWING_DISABLED or SWING_ENABLED.
 *
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "000000B6-0000-1000-8000-0026BB765291", name = "Swing Mode", tag = "swingMode", acceptedItemTypes = {
        "Number", "String" })
public class HomekitSwingModeCharacteristic extends HomekitEnumCharacteristic {
    public enum SwingMode {
        SWING_DISABLED(0),
        SWING_ENABLED(1);

        private final int value;

        SwingMode(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static SwingMode fromValue(int value) {
            for (SwingMode mode : values()) {
                if (mode.value == value) {
                    return mode;
                }
            }
            throw new IllegalArgumentException("Invalid Swing Mode value: " + value);
        }
    }

    public HomekitSwingModeCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, SwingMode.values().length);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(true).withEvents(true)
                .withDescription("Swing Mode");
    }

    public HomekitSwingModeCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        if (value == null) {
            return false;
        }
        try {
            SwingMode.fromValue(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(SwingMode.SWING_DISABLED.getValue(), SwingMode.SWING_ENABLED.getValue());
    }
}
