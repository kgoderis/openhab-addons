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
 * Current Fan State characteristic.
 * This characteristic represents the current state of a fan.
 *
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 * @author Karel Goderis - Initial contribution
 */
@HomekitCharacteristicType(type = "000000AF-0000-1000-8000-0026BB765291", name = "Current Fan State", tag = "currentFanState", acceptedItemTypes = {
        "Number", "String" })
@NonNullByDefault
public class HomekitCurrentFanStateCharacteristic extends HomekitEnumCharacteristic {
    /**
     * Enum representing the possible states of a fan.
     * INACTIVE (0): The fan is not active
     * IDLE (1): The fan is idle
     * BLOWING_AIR (2): The fan is actively blowing air
     */
    public enum FanState {
        INACTIVE(0),
        IDLE(1),
        BLOWING_AIR(2);

        private final int code;

        FanState(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static FanState fromCode(int code) {
            for (FanState s : values()) {
                if (s.code == code) {
                    return s;
                }
            }
            return INACTIVE;
        }
    }

    /**
     * Creates a new Current Fan State characteristic.
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param instanceId The instance ID for this characteristic
     */
    public HomekitCurrentFanStateCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, FanState.values().length);
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(true)
                .withDescription("Current Fan State");
    }

    /**
     * Creates a new Current Fan State characteristic from a JSON value.
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param value The JSON value to initialize the characteristic with
     */
    public HomekitCurrentFanStateCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= 0 && value < FanState.values().length;
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(FanState.INACTIVE.getCode(), FanState.IDLE.getCode(), FanState.BLOWING_AIR.getCode());
    }
}
