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
 * HomeKit Current Heating Cooling State Characteristic.
 * This characteristic represents the current state of a heating/cooling system.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/CurrentHeatingCoolingState">HomeKit Documentation</a>
 * @author Karel Goderis - Initial contribution
 */
@HomekitCharacteristicType(type = "0000000F-0000-1000-8000-0026BB765291", name = "Current Heating Cooling State", tag = "currentHeatingCoolingState", acceptedItemTypes = {
        "Number", "String" })
public class HomekitCurrentHeatingCoolingStateCharacteristic extends HomekitEnumCharacteristic {
    /**
     * Enum representing the possible states of a heating/cooling system.
     * Each state has a corresponding integer code used in the HomeKit protocol.
     */
    public enum HeatingCoolingState {
        /** System is turned off */
        OFF(0),
        /** System is in heating mode */
        HEAT(1),
        /** System is in cooling mode */
        COOL(2),
        /** System is in automatic mode */
        AUTO(3);

        private final int code;

        HeatingCoolingState(int code) {
            this.code = code;
        }

        /**
         * Gets the integer code for this heating/cooling state.
         *
         * @return the integer code
         */
        public int getCode() {
            return code;
        }

        /**
         * Converts an integer code to the corresponding HeatingCoolingState.
         * If no matching state is found, returns OFF as default.
         *
         * @param code the integer code to convert
         * @return the corresponding HeatingCoolingState, or OFF if not found
         */
        public static HeatingCoolingState fromCode(int code) {
            for (HeatingCoolingState s : values()) {
                if (s.code == code) {
                    return s;
                }
            }
            return OFF;
        }
    }

    /**
     * Creates a new Current Heating Cooling State characteristic.
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param instanceId The instance ID for this characteristic
     */
    public HomekitCurrentHeatingCoolingStateCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, HeatingCoolingState.values().length);
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(true)
                .withDescription("Current Heating Cooling State");
    }

    /**
     * Creates a new Current Heating Cooling State characteristic from a JSON value.
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param value The JSON value to initialize the characteristic with
     */
    public HomekitCurrentHeatingCoolingStateCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    /**
     * Checks if the given value is a valid heating/cooling state.
     *
     * @param value the integer value to check
     * @return true if the value corresponds to a valid heating/cooling state, false otherwise
     */
    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= 0 && value < HeatingCoolingState.values().length;
    }

    /**
     * Gets the set of all valid heating/cooling state values.
     *
     * @return a set containing all valid heating/cooling state codes
     */
    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(HeatingCoolingState.OFF.getCode(), HeatingCoolingState.HEAT.getCode(),
                HeatingCoolingState.COOL.getCode(), HeatingCoolingState.AUTO.getCode());
    }
}
