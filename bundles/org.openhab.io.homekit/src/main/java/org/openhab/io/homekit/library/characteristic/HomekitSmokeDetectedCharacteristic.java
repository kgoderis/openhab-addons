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
 * HomeKit Smoke Detected Characteristic.
 * This characteristic represents whether smoke has been detected.
 * The value can be either NOT_DETECTED (0) or DETECTED (1).
 * This is used to indicate the presence of smoke in the environment.
 *
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "00000076-0000-1000-8000-0026BB765291", name = "Smoke Detected", tag = "smokeDetected", acceptedItemTypes = {
        "Number", "String" })
@NonNullByDefault
public class HomekitSmokeDetectedCharacteristic extends HomekitEnumCharacteristic {
    public enum SmokeDetected {
        NOT_DETECTED(0),
        DETECTED(1);

        private final int value;

        SmokeDetected(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static SmokeDetected fromValue(int value) {
            for (SmokeDetected state : values()) {
                if (state.value == value) {
                    return state;
                }
            }
            throw new IllegalArgumentException("Invalid Smoke Detected value: " + value);
        }
    }

    /**
     * Creates a new Smoke Detected characteristic.
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param instanceId The instance ID for this characteristic
     */
    public HomekitSmokeDetectedCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, 2);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(false).withEvents(true)
                .withDescription("Smoke Detected");
    }

    /**
     * Creates a new Smoke Detected characteristic from a JSON value.
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param value The JSON value to initialize the characteristic with
     */
    public HomekitSmokeDetectedCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(@Nullable Integer value) {
        if (value == null)
            return false;
        try {
            SmokeDetected.fromValue(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        java.util.Set<Integer> allowed = new java.util.HashSet<>();
        for (SmokeDetected state : SmokeDetected.values()) {
            allowed.add(state.getValue());
        }
        return allowed;
    }
}
