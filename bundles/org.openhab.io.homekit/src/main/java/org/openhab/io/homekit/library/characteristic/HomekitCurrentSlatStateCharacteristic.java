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
 * Current Slat State characteristic.
 * This characteristic represents the current state of a slat (e.g., in a window blind).
 *
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 * @author Karel Goderis - Initial contribution
 */
@HomekitCharacteristicType(type = "000000AA-0000-1000-8000-0026BB765291", name = "Current Slat State", tag = "currentSlatState", acceptedItemTypes = {
        "Number", "String" })
@NonNullByDefault
public class HomekitCurrentSlatStateCharacteristic extends HomekitEnumCharacteristic {
    /**
     * Enum representing the possible states of a slat.
     * INACTIVE (0): The slat is not active
     * IDLE (1): The slat is idle
     * ROTATING (2): The slat is currently rotating
     */
    public enum SlatState {
        INACTIVE(0),
        IDLE(1),
        ROTATING(2);

        private final int code;

        SlatState(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static SlatState fromCode(int code) {
            for (SlatState s : values()) {
                if (s.code == code) {
                    return s;
                }
            }
            return INACTIVE;
        }
    }

    /**
     * Creates a new Current Slat State characteristic.
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param instanceId The instance ID for this characteristic
     */
    public HomekitCurrentSlatStateCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, SlatState.values().length);
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(true)
                .withDescription("Current Slat State");
    }

    /**
     * Creates a new Current Slat State characteristic from a JSON value.
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param value The JSON value to initialize the characteristic with
     */
    public HomekitCurrentSlatStateCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(@Nullable Integer value) {
        return value != null && value >= 0 && value < SlatState.values().length;
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(SlatState.INACTIVE.getCode(), SlatState.IDLE.getCode(), SlatState.ROTATING.getCode());
    }
}
