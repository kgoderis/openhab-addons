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

import java.util.Set;

import javax.json.JsonValue;

import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Program Mode Characteristic.
 * <p>
 * This characteristic represents the program mode of a device, such as a thermostat, indicating whether a program is
 * scheduled or in manual mode. The value is an integer corresponding to a specific program mode as defined by the HAP
 * specification.
 * <p>
 * See the HomeKit Accessory Protocol (HAP) specification for details: https://developer.apple.com/documentation/HomeKit
 *
 * @author Karel Goderis
     */
@HomekitCharacteristicType(type = "000000D1-0000-1000-8000-0026BB765291", name = "Program Mode", tag = "programMode", acceptedItemTypes = {
        "Number", "String" })
public class HomekitProgramModeCharacteristic extends HomekitIntegerCharacteristic {
    /**
     * Enum representing the possible program modes.
     */
    public enum ProgramMode {
        NO_PROGRAM_SCHEDULED(0),
        PROGRAM_SCHEDULED(1),
        PROGRAM_SCHEDULED_MANUAL_MODE(2);

        private final int code;

        ProgramMode(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static ProgramMode fromCode(int code) {
            for (ProgramMode s : values()) {
                if (s.code == code) {
                    return s;
                }
            }
            return NO_PROGRAM_SCHEDULED;
        }
    }

    /**
     * Constructs a new Program Mode characteristic.
     *
     * @param service the HomeKit service this characteristic belongs to
     * @param eventManager the event manager for handling HomeKit events
     * @param instanceId the instance ID for this characteristic
     */
    public HomekitProgramModeCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 0, 2, "");
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(true)
                .withDescription("Program Mode");
    }

    /**
     * Constructs a new Program Mode characteristic from a JSON value.
     *
     * @param service the HomeKit service this characteristic belongs to
     * @param eventManager the event manager for handling HomeKit events
     * @param value the JSON value to initialize the characteristic with
     */
    public HomekitProgramModeCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    /**
     * Checks if the given value is an allowed program mode.
     *
     * @param value the value to check
     * @return true if the value is allowed, false otherwise
     */
    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && (value == ProgramMode.NO_PROGRAM_SCHEDULED.getCode()
                || value == ProgramMode.PROGRAM_SCHEDULED.getCode()
                || value == ProgramMode.PROGRAM_SCHEDULED_MANUAL_MODE.getCode());
    }

    /**
     * Returns the set of allowed program mode values.
     *
     * @return the set of allowed values
     */
    @Override
    public Set<Integer> getAllowedValues() {
        return Set.of(ProgramMode.NO_PROGRAM_SCHEDULED.getCode(), ProgramMode.PROGRAM_SCHEDULED.getCode(),
                ProgramMode.PROGRAM_SCHEDULED_MANUAL_MODE.getCode());
    }
}
